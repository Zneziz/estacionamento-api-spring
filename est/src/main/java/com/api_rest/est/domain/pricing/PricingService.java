package com.api_rest.est.domain.pricing;

import com.api_rest.est.domain.pricing.dto.PricingRequestDTO;
import com.api_rest.est.domain.pricing.dto.PricingResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PricingService {

    private final PricingRepository repository;


    public PricingService(PricingRepository repository) {
        this.repository = repository;
    }

    public PricingResponseDTO saveOrUpdate(PricingRequestDTO dto) {
        // Procura se já existe tabela para CAR ou MOTORCYCLE; se não existir, instancia uma nova
        PricingTable pricing = repository.findByVehicleType(dto.vehicleType())
                .orElseGet(PricingTable::new);

        pricing.setVehicleType(dto.vehicleType());
        pricing.setFirstHourRate(dto.firstHourRate());
        pricing.setAdditionalFractionRate(dto.additionalFractionRate());
        pricing.setDailyCap(dto.dailyCap());

        PricingTable savedPricing = repository.save(pricing);
        return new PricingResponseDTO(savedPricing);
    }

    public List<PricingResponseDTO> listAll() {
        return repository.findAll().stream()
                .map(PricingResponseDTO::new)
                .toList();
    }
}
