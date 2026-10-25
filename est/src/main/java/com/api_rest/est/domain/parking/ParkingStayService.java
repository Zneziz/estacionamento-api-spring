package com.api_rest.est.domain.parking;

import com.api_rest.est.domain.monthly.MonthlyPlan;
import com.api_rest.est.domain.monthly.MonthlyPlanRepository;
import com.api_rest.est.domain.parking.dto.*;
import com.api_rest.est.domain.partners.PartnerStore;
import com.api_rest.est.domain.partners.PartnerStoreRepository;
import com.api_rest.est.domain.pricing.FeeCalculator;
import com.api_rest.est.domain.pricing.PricingRepository;
import com.api_rest.est.domain.pricing.PricingTable;
import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.spots.SpotType;
import com.api_rest.est.domain.users.Role;
import com.api_rest.est.domain.users.User;
import com.api_rest.est.domain.vehicles.Vehicle;
import com.api_rest.est.domain.vehicles.VehicleRepository;
import com.api_rest.est.domain.vehicles.VehicleType;
import com.api_rest.est.infra.exceptions.BusinessRuleException;
import com.api_rest.est.infra.exceptions.ForbiddenAccessException;
import com.api_rest.est.infra.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParkingStayService {

    private final ParkingStayRepository stayRepository;
    private final VehicleRepository vehicleRepository;
    private final SpotRepository spotRepository;
    private final PricingRepository pricingRepository;
    private final FeeCalculator feeCalculator;
    private final MonthlyPlanRepository monthlyPlanRepository;
    private final PartnerStoreRepository partnerStoreRepository;

    public ParkingStayService(
            ParkingStayRepository stayRepository,
            VehicleRepository vehicleRepository,
            SpotRepository spotRepository,
            PricingRepository pricingRepository,
            FeeCalculator feeCalculator,
            MonthlyPlanRepository monthlyPlanRepository,
            PartnerStoreRepository partnerStoreRepository) {
        this.stayRepository = stayRepository;
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
        this.pricingRepository = pricingRepository;
        this.feeCalculator = feeCalculator;
        this.monthlyPlanRepository = monthlyPlanRepository;
        this.partnerStoreRepository = partnerStoreRepository;
    }

    @Transactional
    public ParkingStayResponseDTO registerEntry(EntryRequestDTO dto) {
        String plate = dto.licensePlate().trim().toUpperCase();

        // Regra RF06: A mesma placa não pode ter duas permanências abertas
        if (stayRepository.existsByVehicleLicensePlateIgnoreCaseAndStatus(plate, ParkingStayStatus.OPEN)) {
            throw new IllegalArgumentException("This vehicle already has an open parking stay.");
        }

        // Busca o veículo pela placa
        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with license plate: " + plate));

        // Determina os tipos de vaga compatíveis com o veículo e o cliente
        List<SpotType> compatibleTypes = getCompatibleSpotTypes(vehicle);

        // Busca a primeira vaga livre compatível; se não houver, recusa a entrada
        Spot allocatedSpot = spotRepository.findFirstByOccupiedFalseAndTypeIn(compatibleTypes)
                .orElseThrow(() -> new IllegalArgumentException("No compatible free spot available. Entry refused."));

        // Ocupa a vaga física
        allocatedSpot.setOccupied(true);
        spotRepository.save(allocatedSpot);

        // Registra a permanência aberta
        ParkingStay stay = new ParkingStay();
        stay.setVehicle(vehicle);
        stay.setSpot(allocatedSpot);
        stay.setEntryTime(LocalDateTime.now());
        stay.setStatus(ParkingStayStatus.OPEN);

        ParkingStay savedStay = stayRepository.save(stay);
        return new ParkingStayResponseDTO(savedStay);
    }

    @Transactional(readOnly = true)
    public CurrentFeeResponseDTO getCurrentFee(String licensePlate, User authenticatedUser) {
        String plate = licensePlate.trim().toUpperCase();

        // Busca a permanência aberta para esta placa
        ParkingStay stay = stayRepository.findByVehicleLicensePlateIgnoreCaseAndStatus(plate, ParkingStayStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No open parking stay found for license plate: " + plate));

        // Regra Geral: O cliente só acessa os próprios veículos e permanências
        if (authenticatedUser.getRole() == Role.CLIENT) {
            Long ownerId = stay.getVehicle().getClient().getId();
            if (!ownerId.equals(authenticatedUser.getId())) {
                throw new ForbiddenAccessException("You do not have permission to view this vehicle's stay.");
            }
        }

        // Busca a tabela de preços vigente para o tipo do veículo (CAR ou MOTORCYCLE)
        PricingTable pricing = pricingRepository.findByVehicleType(stay.getVehicle().getType())
                .orElseThrow(() -> new IllegalArgumentException("Pricing table not configured for vehicle type: " + stay.getVehicle().getType()));

        LocalDateTime now = LocalDateTime.now();
        long elapsedMinutes = java.time.Duration.between(stay.getEntryTime(), now).toMinutes();

        boolean isActiveMonthlyMember = isVehicleCoveredByMonthlyPlan(stay.getVehicle().getId(), now.toLocalDate());

        // Calcula quanto custaria se saísse agora (mensalista em dia paga 0)
        BigDecimal currentAmount = feeCalculator.calculateFee(
                stay.getEntryTime(), now, pricing, 0, isActiveMonthlyMember, false);

        return new CurrentFeeResponseDTO(
                stay.getId(),
                stay.getVehicle().getLicensePlate(),
                stay.getSpot().getCode(),
                stay.getEntryTime(),
                now,
                elapsedMinutes,
                currentAmount
        );
    }

    private List<SpotType> getCompatibleSpotTypes(Vehicle vehicle) {
        List<SpotType> types = new ArrayList<>();

        if (vehicle.getType() == VehicleType.MOTORCYCLE) {
            if (vehicle.isElectric()) {
                types.add(SpotType.ELECTRIC); // Vaga de elétrico só para veículo elétrico
            }
            types.add(SpotType.MOTORCYCLE);
        } else if (vehicle.getType() == VehicleType.CAR) {
            // Vaga PCD só para veículo de cliente com credencial
            String pcdCredential = vehicle.getClient().getPcdCredential();
            if (pcdCredential != null && !pcdCredential.isBlank()) {
                types.add(SpotType.PCD);
            }
            // Vaga de elétrico só para veículo elétrico
            if (vehicle.isElectric()) {
                types.add(SpotType.ELECTRIC);
            }
            types.add(SpotType.CAR);
        }

        return types;
    }

    // RF08 / RF09 / RF10 / RF11: O operador registra a saída, calcula o valor, registra o pagamento e libera a vaga na mesma transação
    @Transactional
    public ExitResponseDTO registerExit(ExitRequestDTO dto) {
        String plate = dto.licensePlate().trim().toUpperCase();

        // 1. Busca a permanência aberta para a placa informada
        ParkingStay stay = stayRepository.findByVehicleLicensePlateIgnoreCaseAndStatus(plate, ParkingStayStatus.OPEN)
                .orElseThrow(() -> new IllegalArgumentException("No open parking stay found for license plate: " + plate));

        // 2. Busca a tabela de preços vigente para o tipo do veículo (CAR ou MOTORCYCLE)
        PricingTable pricing = pricingRepository.findByVehicleType(stay.getVehicle().getType())
                .orElseThrow(() -> new IllegalArgumentException("Pricing table not configured for vehicle type: " + stay.getVehicle().getType()));

        LocalDateTime exitTime = LocalDateTime.now();

        // 3. RF11: Verifica se foi informado um convênio (no máximo um por permanência)
        PartnerStore partnerStore = null;
        int discountHours = 0;
        if (dto.partnerStoreId() != null) {
            partnerStore = partnerStoreRepository.findById(dto.partnerStoreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partner store not found with id: " + dto.partnerStoreId()));
            if (!partnerStore.isActive()) {
                throw new BusinessRuleException("Partner store is not active.");
            }
            discountHours = partnerStore.getDiscountHours();
        }

        // 4. RF10: Verifica se o veículo possui plano mensal ativo com mensalidade em dia
        // Mensalista não paga na saída; com a mensalidade vencida, paga como avulso
        boolean isActiveMonthlyMember = isVehicleCoveredByMonthlyPlan(stay.getVehicle().getId(), exitTime.toLocalDate());

        // 5. RF09: Verifica se houve perda de ticket (cobra uma diária a mais sobre o valor calculado)
        boolean lostTicket = dto.isLostTicket();

        // 6. Calcula o valor final (tolerância de 10 min, 1ª hora, frações de 15 min, teto diário, convênio, mensalista e ticket perdido)
        BigDecimal finalAmount = feeCalculator.calculateFee(
                stay.getEntryTime(),
                exitTime,
                pricing,
                discountHours,
                isActiveMonthlyMember,
                lostTicket
        );

        // 7. Grava o horário de saída, o valor calculado, o pagamento e encerra a permanência
        stay.setExitTime(exitTime);
        stay.setTotalAmount(finalAmount);
        stay.setPaymentMethod(dto.paymentMethod());
        stay.setPaid(true);
        stay.setPaidAt(exitTime);
        stay.setLostTicket(lostTicket);
        stay.setMonthlyPlanApplied(isActiveMonthlyMember);
        stay.setPartnerStore(partnerStore);
        stay.setStatus(ParkingStayStatus.CLOSED);

        // 8. Libera a vaga física
        Spot spot = stay.getSpot();
        spot.setOccupied(false);

        // Salva ambos dentro da mesma transação (@Transactional garante atomicidade: tudo ou nada)
        spotRepository.save(spot);
        ParkingStay savedStay = stayRepository.save(stay);

        return new ExitResponseDTO(savedStay);
    }

    // RF12: O cliente vê o histórico de permanências e os valores pagos dos próprios veículos
    @Transactional(readOnly = true)
    public List<ParkingStayHistoryResponseDTO> getClientHistory(User authenticatedUser, String licensePlate) {
        if (licensePlate != null && !licensePlate.isBlank()) {
            String plate = licensePlate.trim().toUpperCase();
            Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with license plate: " + plate));

            if (!vehicle.getClient().getId().equals(authenticatedUser.getId())) {
                throw new ForbiddenAccessException("You do not have permission to view this vehicle's stay history.");
            }

            return stayRepository.findAllByVehicleLicensePlateIgnoreCaseOrderByEntryTimeDesc(plate)
                    .stream()
                    .map(ParkingStayHistoryResponseDTO::new)
                    .toList();
        }

        return stayRepository.findAllByVehicleClientIdOrderByEntryTimeDesc(authenticatedUser.getId())
                .stream()
                .map(ParkingStayHistoryResponseDTO::new)
                .toList();
    }

    private boolean isVehicleCoveredByMonthlyPlan(Long vehicleId, LocalDate referenceDate) {
        return monthlyPlanRepository.findFirstByVehicleIdAndActiveTrueOrderByExpirationDateDesc(vehicleId)
                .map(plan -> plan.isValidAt(referenceDate))
                .orElse(false);
    }
}
