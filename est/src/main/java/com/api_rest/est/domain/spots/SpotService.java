package com.api_rest.est.domain.spots;

import com.api_rest.est.domain.spots.dto.SpotRequestDTO;
import com.api_rest.est.domain.spots.dto.SpotResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class SpotService {

    private final SpotRepository repository;

    public SpotService(SpotRepository repository) {
        this.repository = repository;
    }

    public SpotResponseDTO create(SpotRequestDTO dto) {
        if (repository.existsByCode(dto.code())) {
            throw new IllegalArgumentException("Spot code already exists.");
        }

        Spot spot = new Spot();
        spot.setCode(dto.code());
        spot.setType(dto.type());
        spot.setOccupied(false);

        Spot savedSpot = repository.save(spot);
        return new SpotResponseDTO(savedSpot);
    }
}
