package com.api_rest.est.domain.partners;

import com.api_rest.est.domain.partners.dto.PartnerStoreRequestDTO;
import com.api_rest.est.domain.partners.dto.PartnerStoreResponseDTO;
import com.api_rest.est.infra.exceptions.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartnerStoreService {

    private final PartnerStoreRepository repository;

    public PartnerStoreService(PartnerStoreRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PartnerStoreResponseDTO create(PartnerStoreRequestDTO dto) {
        String trimmedName = dto.name().trim();
        if (repository.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Partner store with this name already exists.");
        }

        PartnerStore store = new PartnerStore();
        store.setName(trimmedName);
        store.setDiscountHours(dto.discountHours());
        store.setActive(true);

        PartnerStore saved = repository.save(store);
        return new PartnerStoreResponseDTO(saved);
    }

    public List<PartnerStoreResponseDTO> listAll() {
        return repository.findAll().stream()
                .map(PartnerStoreResponseDTO::new)
                .toList();
    }
}
