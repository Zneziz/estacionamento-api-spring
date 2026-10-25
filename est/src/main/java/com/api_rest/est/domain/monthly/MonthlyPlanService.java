package com.api_rest.est.domain.monthly;

import com.api_rest.est.domain.monthly.dto.MonthlyPlanRequestDTO;
import com.api_rest.est.domain.monthly.dto.MonthlyPlanResponseDTO;
import com.api_rest.est.domain.pricing.PricingRepository;
import com.api_rest.est.domain.pricing.PricingTable;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.users.User;
import com.api_rest.est.domain.vehicles.Vehicle;
import com.api_rest.est.domain.vehicles.VehicleRepository;
import com.api_rest.est.infra.exceptions.BusinessRuleException;
import com.api_rest.est.infra.exceptions.ConflictException;
import com.api_rest.est.infra.exceptions.ForbiddenAccessException;
import com.api_rest.est.infra.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonthlyPlanService {

    private static final double MAX_MONTHLY_RATIO = 0.30;
    private static final BigDecimal DEFAULT_MONTHLY_FEE = new BigDecimal("250.00");

    private final MonthlyPlanRepository monthlyPlanRepository;
    private final VehicleRepository vehicleRepository;
    private final SpotRepository spotRepository;
    private final PricingRepository pricingRepository;

    public MonthlyPlanService(
            MonthlyPlanRepository monthlyPlanRepository,
            VehicleRepository vehicleRepository,
            SpotRepository spotRepository,
            PricingRepository pricingRepository) {
        this.monthlyPlanRepository = monthlyPlanRepository;
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
        this.pricingRepository = pricingRepository;
    }

    @Transactional
    public MonthlyPlanResponseDTO contractPlan(MonthlyPlanRequestDTO dto, User authenticatedUser) {
        String plate = dto.licensePlate().trim().toUpperCase();

        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with license plate: " + plate));

        // Regra Geral / RF10: O cliente só pode contratar plano para um veículo seu
        if (!vehicle.getClient().getId().equals(authenticatedUser.getId())) {
            throw new ForbiddenAccessException("You can only contract a monthly plan for your own vehicle.");
        }

        LocalDate today = LocalDate.now();

        // Verifica se já existe um plano mensal ativo e vigente para este veículo
        if (monthlyPlanRepository.existsByVehicleIdAndActiveTrueAndExpirationDateGreaterThanEqual(vehicle.getId(), today)) {
            throw new ConflictException("This vehicle already has an active monthly plan.");
        }

        // RF10: O número de mensalistas ativos não passa de 30% das vagas
        long totalSpots = spotRepository.count();
        long maxMonthlyPlans = (long) Math.floor(totalSpots * MAX_MONTHLY_RATIO);
        long currentActivePlans = monthlyPlanRepository.countByActiveTrueAndExpirationDateGreaterThanEqual(today);

        if (currentActivePlans >= maxMonthlyPlans) {
            throw new BusinessRuleException(
                    "Monthly plan limit reached: active monthly members cannot exceed 30% of total spots.");
        }

        LocalDate startDate = dto.startDate() != null ? dto.startDate() : today;
        LocalDate expirationDate = dto.expirationDate() != null ? dto.expirationDate() : startDate.plusDays(30);

        BigDecimal monthlyFee = resolveMonthlyFee(dto.monthlyFee(), vehicle);

        MonthlyPlan plan = new MonthlyPlan();
        plan.setVehicle(vehicle);
        plan.setClient(vehicle.getClient());
        plan.setMonthlyFee(monthlyFee);
        plan.setStartDate(startDate);
        plan.setExpirationDate(expirationDate);
        plan.setContractedAt(LocalDateTime.now());
        plan.setActive(true);

        MonthlyPlan savedPlan = monthlyPlanRepository.save(plan);
        return new MonthlyPlanResponseDTO(savedPlan);
    }

    @Transactional(readOnly = true)
    public List<MonthlyPlanResponseDTO> listMyPlans(User authenticatedUser) {
        return monthlyPlanRepository.findAllByClientIdOrderByContractedAtDesc(authenticatedUser.getId())
                .stream()
                .map(MonthlyPlanResponseDTO::new)
                .toList();
    }

    private BigDecimal resolveMonthlyFee(BigDecimal requestedFee, Vehicle vehicle) {
        if (requestedFee != null) {
            return requestedFee.setScale(2, RoundingMode.HALF_UP);
        }
        return pricingRepository.findByVehicleType(vehicle.getType())
                .map(PricingTable::getDailyCap)
                .map(cap -> cap.multiply(BigDecimal.valueOf(5)).setScale(2, RoundingMode.HALF_UP))
                .orElse(DEFAULT_MONTHLY_FEE);
    }
}
