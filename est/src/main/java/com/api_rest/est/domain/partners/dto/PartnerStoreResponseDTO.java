package com.api_rest.est.domain.partners.dto;

import com.api_rest.est.domain.partners.PartnerStore;

public record PartnerStoreResponseDTO(
        Long id,
        String name,
        int discountHours,
        boolean active
) {
    public PartnerStoreResponseDTO(PartnerStore store) {
        this(store.getId(), store.getName(), store.getDiscountHours(), store.isActive());
    }
}
