package com.api_rest.est.domain.spots.dto;

import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.spots.SpotType;

public record SpotResponseDTO(Long id, String code, SpotType type, boolean occupied) {

    public SpotResponseDTO(Spot spot) {
        this(spot.getId(), spot.getCode(), spot.getType(), spot.isOccupied());
    }
}