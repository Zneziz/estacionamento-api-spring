package com.api_rest.est.domain.parking.dto;

import jakarta.validation.constraints.NotBlank;

public record EntryRequestDTO(
        @NotBlank(message = "License plate is required")
        String licensePlate
) {}
