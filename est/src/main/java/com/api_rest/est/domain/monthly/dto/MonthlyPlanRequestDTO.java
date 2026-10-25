package com.api_rest.est.domain.monthly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyPlanRequestDTO(
        @NotBlank(message = "License plate is required")
        String licensePlate,

        @Positive(message = "Monthly fee must be greater than zero")
        BigDecimal monthlyFee,

        LocalDate startDate,

        LocalDate expirationDate
) {
    public MonthlyPlanRequestDTO(String licensePlate) {
        this(licensePlate, null, null, null);
    }
}
