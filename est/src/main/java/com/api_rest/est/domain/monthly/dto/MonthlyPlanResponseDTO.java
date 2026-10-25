package com.api_rest.est.domain.monthly.dto;

import com.api_rest.est.domain.monthly.MonthlyPlan;
import com.api_rest.est.domain.vehicles.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MonthlyPlanResponseDTO(
        Long id,
        String licensePlate,
        VehicleType vehicleType,
        Long clientId,
        BigDecimal monthlyFee,
        LocalDate startDate,
        LocalDate expirationDate,
        LocalDateTime contractedAt,
        boolean active,
        boolean expired
) {
    public MonthlyPlanResponseDTO(MonthlyPlan plan) {
        this(
                plan.getId(),
                plan.getVehicle().getLicensePlate(),
                plan.getVehicle().getType(),
                plan.getClient().getId(),
                plan.getMonthlyFee(),
                plan.getStartDate(),
                plan.getExpirationDate(),
                plan.getContractedAt(),
                plan.isActive(),
                plan.getExpirationDate().isBefore(LocalDate.now())
        );
    }
}
