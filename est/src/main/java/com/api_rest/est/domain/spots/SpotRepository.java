package com.api_rest.est.domain.spots;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotRepository extends JpaRepository<Spot, Long> {
    boolean existsByCode(String code);
}
