package com.api_rest.est.domain.parking.dto;

import com.api_rest.est.domain.parking.ParkingStay;
import com.api_rest.est.domain.parking.ParkingStayStatus;
import com.api_rest.est.domain.parking.PaymentMethod;
import com.api_rest.est.domain.vehicles.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record ParkingStayHistoryResponseDTO(
        Long stayId,
        String licensePlate,
        VehicleType vehicleType,
        String spotCode,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        Long durationMinutes,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        boolean paid,
        boolean lostTicket,
        boolean monthlyPlanApplied,
        String partnerStoreName,
        ParkingStayStatus status
) {
    public ParkingStayHistoryResponseDTO(ParkingStay stay) {
        this(
                stay.getId(),
                stay.getVehicle().getLicensePlate(),
                stay.getVehicle().getType(),
                stay.getSpot().getCode(),
                stay.getEntryTime(),
                stay.getExitTime(),
                stay.getExitTime() != null
                        ? Duration.between(stay.getEntryTime(), stay.getExitTime()).toMinutes()
                        : null,
                stay.getTotalAmount(),
                stay.getPaymentMethod(),
                stay.isPaid(),
                stay.isLostTicket(),
                stay.isMonthlyPlanApplied(),
                stay.getPartnerStore() != null ? stay.getPartnerStore().getName() : null,
                stay.getStatus()
        );
    }
}
