package com.api_rest.est.domain.reports.dto;

import com.api_rest.est.domain.spots.SpotType;

public record OccupancyByTypeDTO(
        SpotType spotType,
        long totalSpots,
        long occupiedSpots,
        long freeSpots,
        double occupancyRatePercentage
) {}
