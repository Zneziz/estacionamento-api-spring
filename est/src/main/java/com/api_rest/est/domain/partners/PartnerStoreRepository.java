package com.api_rest.est.domain.partners;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerStoreRepository extends JpaRepository<PartnerStore, Long> {
    boolean existsByNameIgnoreCase(String name);
}
