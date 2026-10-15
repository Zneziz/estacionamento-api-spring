package com.api_rest.est.domain.spots.dto;

import com.api_rest.est.domain.spots.SpotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpotRequestDTO(
        @NotBlank(message = "The spot code is required")
        String code,

        @NotNull(message = "The spot type is required")
        SpotType type
) {}
