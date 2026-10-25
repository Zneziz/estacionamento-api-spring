package com.api_rest.est.domain.parking.dto;

import com.api_rest.est.domain.parking.ParkingStay;
import com.api_rest.est.domain.parking.ParkingStayStatus;
import com.api_rest.est.domain.parking.PaymentMethod;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record ExitResponseDTO(
        Long stayId,
        String licensePlate,
        String spotCode,
        boolean spotReleased,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        long totalMinutes,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        boolean paid,
        boolean lostTicket,
        boolean monthlyPlanApplied,
        Long partnerStoreId,
        String partnerStoreName,
        int discountHoursApplied,
        ParkingStayStatus status
) {
    public ExitResponseDTO(ParkingStay stay) {
        this(
                stay.getId(),
                stay.getVehicle().getLicensePlate(),
                stay.getSpot().getCode(),
                !stay.getSpot().isOccupied(), // true se a vaga foi liberada
                stay.getEntryTime(),
                stay.getExitTime(),
                Duration.between(stay.getEntryTime(), stay.getExitTime()).toMinutes(),
                stay.getTotalAmount(),
                stay.getPaymentMethod(),
                stay.isPaid(),
                stay.isLostTicket(),
                stay.isMonthlyPlanApplied(),
                stay.getPartnerStore() != null ? stay.getPartnerStore().getId() : null,
                stay.getPartnerStore() != null ? stay.getPartnerStore().getName() : null,
                stay.getPartnerStore() != null ? stay.getPartnerStore().getDiscountHours() : 0,
                stay.getStatus()
        );
    }
}
