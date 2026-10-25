package com.api_rest.est.domain.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueDTO(
        LocalDate date,
        BigDecimal casualRevenue,
        BigDecimal monthlyRevenue,
        BigDecimal partnerRevenue,
        BigDecimal totalRevenue
) {}
