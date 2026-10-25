package com.api_rest.est.unit;

import com.api_rest.est.domain.monthly.MonthlyPlan;
import com.api_rest.est.domain.monthly.MonthlyPlanRepository;
import com.api_rest.est.domain.monthly.MonthlyPlanService;
import com.api_rest.est.domain.monthly.dto.MonthlyPlanRequestDTO;
import com.api_rest.est.domain.monthly.dto.MonthlyPlanResponseDTO;
import com.api_rest.est.domain.pricing.PricingRepository;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.users.Client;
import com.api_rest.est.domain.users.Role;
import com.api_rest.est.domain.vehicles.Vehicle;
import com.api_rest.est.domain.vehicles.VehicleRepository;
import com.api_rest.est.domain.vehicles.VehicleType;
import com.api_rest.est.infra.exceptions.BusinessRuleException;
import com.api_rest.est.infra.exceptions.ConflictException;
import com.api_rest.est.infra.exceptions.ForbiddenAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyPlanServiceTest {

    @Mock
    private MonthlyPlanRepository monthlyPlanRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private SpotRepository spotRepository;
    @Mock
    private PricingRepository pricingRepository;

    @InjectMocks
    private MonthlyPlanService service;

    private Client owner;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        owner = new Client();
        owner.setId(1L);
        owner.setName("Pedro");
        owner.setCpf("12345678901");
        owner.setEmail("pedro@email.com");
        owner.setRole(Role.CLIENT);

        vehicle = new Vehicle(10L, "ABC1D23", VehicleType.CAR, false, owner);
    }

    @Test
    @DisplayName("RF10: Deve contratar plano mensal quando dentro do limite de 30% das vagas")
    void shouldContractMonthlyPlanWithin30PercentLimit() {
        when(vehicleRepository.findByLicensePlateIgnoreCase("ABC1D23")).thenReturn(Optional.of(vehicle));
        when(monthlyPlanRepository.existsByVehicleIdAndActiveTrueAndExpirationDateGreaterThanEqual(eq(10L), any(LocalDate.class)))
                .thenReturn(false);
        // 10 vagas totais -> 30% = 3 mensalistas permitidos; atualmente 2 ativos
        when(spotRepository.count()).thenReturn(10L);
        when(monthlyPlanRepository.countByActiveTrueAndExpirationDateGreaterThanEqual(any(LocalDate.class)))
                .thenReturn(2L);
        when(monthlyPlanRepository.save(any(MonthlyPlan.class))).thenAnswer(inv -> {
            MonthlyPlan p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        MonthlyPlanRequestDTO dto = new MonthlyPlanRequestDTO("ABC1D23", new BigDecimal("250.00"), null, null);
        MonthlyPlanResponseDTO response = service.contractPlan(dto, owner);

        assertNotNull(response);
        assertEquals("ABC1D23", response.licensePlate());
        assertEquals(new BigDecimal("250.00"), response.monthlyFee());
        assertTrue(response.active());
        assertFalse(response.expired());
    }

    @Test
    @DisplayName("RF10: Deve recusar contratação quando atingir o limite de 30% das vagas")
    void shouldRefuseWhenExceeding30PercentOfTotalSpots() {
        when(vehicleRepository.findByLicensePlateIgnoreCase("ABC1D23")).thenReturn(Optional.of(vehicle));
        when(monthlyPlanRepository.existsByVehicleIdAndActiveTrueAndExpirationDateGreaterThanEqual(eq(10L), any(LocalDate.class)))
                .thenReturn(false);
        // 10 vagas totais -> 30% = 3 mensalistas permitidos; já existem 3 ativos
        when(spotRepository.count()).thenReturn(10L);
        when(monthlyPlanRepository.countByActiveTrueAndExpirationDateGreaterThanEqual(any(LocalDate.class)))
                .thenReturn(3L);

        MonthlyPlanRequestDTO dto = new MonthlyPlanRequestDTO("ABC1D23");

        assertThrows(BusinessRuleException.class, () -> service.contractPlan(dto, owner));
    }

    @Test
    @DisplayName("RF10 / Regra Geral: Cliente não pode contratar plano para veículo de outro cliente")
    void shouldForbidContractingPlanForAnotherClientsVehicle() {
        Client anotherClient = new Client();
        anotherClient.setId(2L);
        anotherClient.setRole(Role.CLIENT);

        when(vehicleRepository.findByLicensePlateIgnoreCase("ABC1D23")).thenReturn(Optional.of(vehicle));

        MonthlyPlanRequestDTO dto = new MonthlyPlanRequestDTO("ABC1D23");

        assertThrows(ForbiddenAccessException.class, () -> service.contractPlan(dto, anotherClient));
    }

    @Test
    @DisplayName("RF10: Não deve permitir contratar segundo plano ativo para o mesmo veículo")
    void shouldRejectDuplicateActivePlanForSameVehicle() {
        when(vehicleRepository.findByLicensePlateIgnoreCase("ABC1D23")).thenReturn(Optional.of(vehicle));
        when(monthlyPlanRepository.existsByVehicleIdAndActiveTrueAndExpirationDateGreaterThanEqual(eq(10L), any(LocalDate.class)))
                .thenReturn(true);

        MonthlyPlanRequestDTO dto = new MonthlyPlanRequestDTO("ABC1D23");

        assertThrows(ConflictException.class, () -> service.contractPlan(dto, owner));
    }
}
