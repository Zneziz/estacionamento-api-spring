package com.api_rest.est.domain.reports.dto;

public record AverageStayTimeBySlotDTO(
        String timeSlot,
        int startHour,
        int endHour,
        long stayCount,
        double averageDurationMinutes
) {}
