package com.api_rest.est.domain.vehicles.dto;

import com.api_rest.est.domain.vehicles.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequestDTO(
        @NotBlank(message = "License plate is required")
        String licensePlate,

        @NotNull(message = "Vehicle type is required")
        VehicleType type,

        @NotNull(message = "Electric status is required")
        Boolean electric,

        // Opcional: Cliente com credencial PCD informa o número da credencial
        String pcdCredential
) {}
