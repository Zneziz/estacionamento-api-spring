package com.api_rest.est.domain.parking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CurrentFeeResponseDTO(
        Long stayId,
        String licensePlate,
        String spotCode,
        LocalDateTime entryTime,
        LocalDateTime consultationTime,
        long elapsedMinutes,
        BigDecimal currentAmount
) {}
