package com.api_rest.est.domain.parking.dto;

import com.api_rest.est.domain.parking.ParkingStay;
import com.api_rest.est.domain.parking.ParkingStayStatus;
import com.api_rest.est.domain.spots.SpotType;
import com.api_rest.est.domain.vehicles.VehicleType;

import java.time.LocalDateTime;

public record ParkingStayResponseDTO(
        Long stayId,
        String licensePlate,
        VehicleType vehicleType,
        String spotCode,
        SpotType spotType,
        LocalDateTime entryTime,
        ParkingStayStatus status
) {
    public ParkingStayResponseDTO(ParkingStay stay) {
        this(
                stay.getId(),
                stay.getVehicle().getLicensePlate(),
                stay.getVehicle().getType(),
                stay.getSpot().getCode(),
                stay.getSpot().getType(),
                stay.getEntryTime(),
                stay.getStatus()
        );
    }
}
