package com.api_rest.est.domain.reports.dto;

import java.util.List;

public record OccupancyReportResponseDTO(
        long totalSpots,
        long totalOccupied,
        long totalFree,
        double overallOccupancyRatePercentage,
        List<OccupancyByTypeDTO> byType
) {}
