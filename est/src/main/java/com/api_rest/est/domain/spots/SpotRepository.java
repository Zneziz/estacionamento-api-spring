package com.api_rest.est.domain.spots;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, Long> {
    boolean existsByCode(String code);

    // Busca a primeira vaga desocupada cujo tipo esteja dentro da lista de tipos compatíveis
    Optional<Spot> findFirstByOccupiedFalseAndTypeIn(List<SpotType> compatibleTypes);
}
