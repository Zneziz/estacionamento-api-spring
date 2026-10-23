package com.api_rest.est.domain.pricing.dto;

import com.api_rest.est.domain.pricing.PricingTable;
import com.api_rest.est.domain.vehicles.VehicleType;

import java.math.BigDecimal;

public record PricingResponseDTO(
        Long id,
        VehicleType vehicleType,
        BigDecimal firstHourRate,
        BigDecimal additionalFractionRate,
        BigDecimal dailyCap,
        int toleranceMinutes
) {
    public PricingResponseDTO(PricingTable pricing) {
        this(
                pricing.getId(),
                pricing.getVehicleType(),
                pricing.getFirstHourRate(),
                pricing.getAdditionalFractionRate(),
                pricing.getDailyCap(),
                PricingTable.TOLERANCE_MINUTES
        );
    }
}
