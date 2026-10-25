package com.api_rest.est.unit;

import com.api_rest.est.domain.monthly.MonthlyPlan;
import com.api_rest.est.domain.monthly.MonthlyPlanRepository;
import com.api_rest.est.domain.parking.*;
import com.api_rest.est.domain.parking.dto.*;
import com.api_rest.est.domain.partners.PartnerStore;
import com.api_rest.est.domain.partners.PartnerStoreRepository;
import com.api_rest.est.domain.pricing.FeeCalculator;
import com.api_rest.est.domain.pricing.PricingRepository;
import com.api_rest.est.domain.pricing.PricingTable;
import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.spots.SpotType;
import com.api_rest.est.domain.users.Client;
import com.api_rest.est.domain.users.Role;
import com.api_rest.est.domain.vehicles.Vehicle;
import com.api_rest.est.domain.vehicles.VehicleRepository;
import com.api_rest.est.domain.vehicles.VehicleType;
import com.api_rest.est.infra.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingStayServiceTest {

    @Mock
    private ParkingStayRepository stayRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private SpotRepository spotRepository;
    @Mock
    private PricingRepository pricingRepository;
    @Mock
    private MonthlyPlanRepository monthlyPlanRepository;
    @Mock
    private PartnerStoreRepository partnerStoreRepository;

    private ParkingStayService service;

    private Client client;
    private Vehicle car;
    private Spot spot;
    private PricingTable carPricing;

    @BeforeEach
    void setUp() {
        FeeCalculator feeCalculator = new FeeCalculator();
        service = new ParkingStayService(
                stayRepository,
                vehicleRepository,
                spotRepository,
                pricingRepository,
                feeCalculator,
                monthlyPlanRepository,
                partnerStoreRepository
        );

        client = new Client();
        client.setId(1L);
        client.setName("Pedro");
        client.setCpf("11122233344");
        client.setEmail("pedro@email.com");
        client.setRole(Role.CLIENT);

        car = new Vehicle(1L, "ABC1234", VehicleType.CAR, false, client);
        spot = new Spot(1L, "A-01", SpotType.CAR, false);
        carPricing = new PricingTable(
                1L,
                VehicleType.CAR,
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("50.00")
        );
    }

    @Test
    @DisplayName("RF06: Deve registrar entrada, ocupar vaga compatível e criar permanência aberta")
    void shouldRegisterEntryAndOccupySpot() {
        when(stayRepository.existsByVehicleLicensePlateIgnoreCaseAndStatus("ABC1234", ParkingStayStatus.OPEN))
                .thenReturn(false);
        when(vehicleRepository.findByLicensePlateIgnoreCase("ABC1234")).thenReturn(Optional.of(car));
        when(spotRepository.findFirstByOccupiedFalseAndTypeIn(any())).thenReturn(Optional.of(spot));
        when(stayRepository.save(any(ParkingStay.class))).thenAnswer(inv -> {
            ParkingStay s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        ParkingStayResponseDTO response = service.registerEntry(new EntryRequestDTO("ABC1234"));

        assertEquals(50L, response.stayId());
        assertEquals("A-01", response.spotCode());
        assertEquals(ParkingStayStatus.OPEN, response.status());
        assertTrue(spot.isOccupied());
    }

    @Test
    @DisplayName("RF08 / RF09: Saída com ticket perdido deve cobrar uma diária extra e liberar a vaga")
    void shouldChargeExtraDailyCapOnLostTicketAndReleaseSpot() {
        spot.setOccupied(true);
        ParkingStay openStay = new ParkingStay();
        openStay.setId(10L);
        openStay.setVehicle(car);
        openStay.setSpot(spot);
        openStay.setEntryTime(LocalDateTime.now().minusMinutes(40)); // 40 min -> 1ª hora (10.00)
        openStay.setStatus(ParkingStayStatus.OPEN);

        when(stayRepository.findByVehicleLicensePlateIgnoreCaseAndStatus("ABC1234", ParkingStayStatus.OPEN))
                .thenReturn(Optional.of(openStay));
        when(pricingRepository.findByVehicleType(VehicleType.CAR)).thenReturn(Optional.of(carPricing));
        when(monthlyPlanRepository.findFirstByVehicleIdAndActiveTrueOrderByExpirationDateDesc(1L))
                .thenReturn(Optional.empty());
        when(stayRepository.save(any(ParkingStay.class))).thenAnswer(inv -> inv.getArgument(0));

        ExitRequestDTO exitDto = new ExitRequestDTO("ABC1234", PaymentMethod.PIX, true, null);
        ExitResponseDTO response = service.registerExit(exitDto);

        // 10.00 (1ª hora) + 50.00 (diária extra de ticket perdido) = 60.00
        assertEquals(new BigDecimal("60.00"), response.totalAmount());
        assertTrue(response.lostTicket());
        assertTrue(response.spotReleased());
        assertFalse(spot.isOccupied());
        assertEquals(ParkingStayStatus.CLOSED, response.status());
    }

    @Test
    @DisplayName("RF10: Mensalista com mensalidade vencida deve pagar como avulso na saída")
    void shouldChargeExpiredMonthlyMemberAsCasual() {
        spot.setOccupied(true);
        ParkingStay openStay = new ParkingStay();
        openStay.setId(11L);
        openStay.setVehicle(car);
        openStay.setSpot(spot);
        openStay.setEntryTime(LocalDateTime.now().minusMinutes(45));
        openStay.setStatus(ParkingStayStatus.OPEN);

        MonthlyPlan expiredPlan = new MonthlyPlan(
                1L, car, client, new BigDecimal("250.00"),
                LocalDate.now().minusDays(40),
                LocalDate.now().minusDays(5), // vencido há 5 dias
                LocalDateTime.now().minusDays(40),
                true
        );

        when(stayRepository.findByVehicleLicensePlateIgnoreCaseAndStatus("ABC1234", ParkingStayStatus.OPEN))
                .thenReturn(Optional.of(openStay));
        when(pricingRepository.findByVehicleType(VehicleType.CAR)).thenReturn(Optional.of(carPricing));
        when(monthlyPlanRepository.findFirstByVehicleIdAndActiveTrueOrderByExpirationDateDesc(1L))
                .thenReturn(Optional.of(expiredPlan));
        when(stayRepository.save(any(ParkingStay.class))).thenAnswer(inv -> inv.getArgument(0));

        ExitResponseDTO response = service.registerExit(new ExitRequestDTO("ABC1234", PaymentMethod.CREDIT_CARD));

        assertEquals(new BigDecimal("10.00"), response.totalAmount());
        assertFalse(response.monthlyPlanApplied());
    }

    @Test
    @DisplayName("RF11: Saída com convênio deve abater as horas de desconto antes do cálculo")
    void shouldApplyPartnerStoreDiscountOnExit() {
        spot.setOccupied(true);
        ParkingStay openStay = new ParkingStay();
        openStay.setId(12L);
        openStay.setVehicle(car);
        openStay.setSpot(spot);
        openStay.setEntryTime(LocalDateTime.now().minusMinutes(90)); // 1h30
        openStay.setStatus(ParkingStayStatus.OPEN);

        PartnerStore partnerStore = new PartnerStore(5L, "Supermercado Vizinho", 2, true);

        when(stayRepository.findByVehicleLicensePlateIgnoreCaseAndStatus("ABC1234", ParkingStayStatus.OPEN))
                .thenReturn(Optional.of(openStay));
        when(pricingRepository.findByVehicleType(VehicleType.CAR)).thenReturn(Optional.of(carPricing));
        when(partnerStoreRepository.findById(5L)).thenReturn(Optional.of(partnerStore));
        when(monthlyPlanRepository.findFirstByVehicleIdAndActiveTrueOrderByExpirationDateDesc(1L))
                .thenReturn(Optional.empty());
        when(stayRepository.save(any(ParkingStay.class))).thenAnswer(inv -> inv.getArgument(0));

        ExitResponseDTO response = service.registerExit(
                new ExitRequestDTO("ABC1234", PaymentMethod.DEBIT_CARD, false, 5L));

        assertEquals(new BigDecimal("0.00"), response.totalAmount());
        assertEquals("Supermercado Vizinho", response.partnerStoreName());
        assertEquals(2, response.discountHoursApplied());
    }

    @Test
    @DisplayName("RF12: Cliente não pode acessar histórico de veículo de outro cliente")
    void shouldForbidClientFromViewingAnotherClientsHistory() {
        Client otherClient = new Client();
        otherClient.setId(99L);
        otherClient.setRole(Role.CLIENT);

        when(vehicleRepository.findByLicensePlateIgnoreCase("ABC1234")).thenReturn(Optional.of(car));

        assertThrows(ForbiddenAccessException.class,
                () -> service.getClientHistory(otherClient, "ABC1234"));
    }

    @Test
    @DisplayName("RF12: Cliente pode listar histórico de permanências dos próprios veículos")
    void shouldReturnClientStayHistory() {
        ParkingStay closedStay = new ParkingStay();
        closedStay.setId(20L);
        closedStay.setVehicle(car);
        closedStay.setSpot(spot);
        closedStay.setEntryTime(LocalDateTime.now().minusHours(2));
        closedStay.setExitTime(LocalDateTime.now().minusHours(1));
        closedStay.setTotalAmount(new BigDecimal("10.00"));
        closedStay.setPaymentMethod(PaymentMethod.PIX);
        closedStay.setPaid(true);
        closedStay.setStatus(ParkingStayStatus.CLOSED);

        when(stayRepository.findAllByVehicleClientIdOrderByEntryTimeDesc(1L))
                .thenReturn(List.of(closedStay));

        List<ParkingStayHistoryResponseDTO> history = service.getClientHistory(client, null);

        assertEquals(1, history.size());
        assertEquals(new BigDecimal("10.00"), history.get(0).totalAmount());
        assertEquals("ABC1234", history.get(0).licensePlate());
    }
}
