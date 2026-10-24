package com.api_rest.est.domain.parking.dto;

import com.api_rest.est.domain.parking.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExitRequestDTO(
        @NotBlank(message = "License plate is required")
        String licensePlate,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {}
