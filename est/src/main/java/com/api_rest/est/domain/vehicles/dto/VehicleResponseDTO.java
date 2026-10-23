package com.api_rest.est.domain.vehicles.dto;


import com.api_rest.est.domain.vehicles.Vehicle;
import com.api_rest.est.domain.vehicles.VehicleType;

public record VehicleResponseDTO(
        Long id,
        String licensePlate,
        VehicleType type,
        boolean electric,
        Long clientId,
        String pcdCredential
) {
    public VehicleResponseDTO(Vehicle vehicle) {
        this(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getType(),
                vehicle.isElectric(),
                vehicle.getClient().getId(),
                vehicle.getClient().getPcdCredential()
        );
    }
}
