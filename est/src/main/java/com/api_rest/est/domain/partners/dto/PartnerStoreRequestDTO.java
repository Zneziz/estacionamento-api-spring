package com.api_rest.est.domain.partners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PartnerStoreRequestDTO(
        @NotBlank(message = "Partner store name is required")
        String name,

        @NotNull(message = "Discount hours is required")
        @Positive(message = "Discount hours must be greater than zero")
        Integer discountHours
) {}
