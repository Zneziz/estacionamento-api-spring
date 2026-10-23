package com.api_rest.est.domain.pricing.dto;

import com.api_rest.est.domain.vehicles.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PricingRequestDTO(
        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotNull(message = "First hour rate is required")
        @Positive(message = "First hour rate must be greater than zero")
        BigDecimal firstHourRate,

        @NotNull(message = "Additional 15-min fraction rate is required")
        @Positive(message = "Additional fraction rate must be greater than zero")
        BigDecimal additionalFractionRate,

        @NotNull(message = "Daily cap is required")
        @Positive(message = "Daily cap must be greater than zero")
        BigDecimal dailyCap
) {}