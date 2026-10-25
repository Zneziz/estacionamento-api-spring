package com.api_rest.est.domain.monthly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MonthlyPlanRepository extends JpaRepository<MonthlyPlan, Long> {

    long countByActiveTrueAndExpirationDateGreaterThanEqual(LocalDate referenceDate);

    boolean existsByVehicleIdAndActiveTrueAndExpirationDateGreaterThanEqual(Long vehicleId, LocalDate referenceDate);

    Optional<MonthlyPlan> findFirstByVehicleIdAndActiveTrueOrderByExpirationDateDesc(Long vehicleId);

    List<MonthlyPlan> findAllByClientIdOrderByContractedAtDesc(Long clientId);

    List<MonthlyPlan> findAllByContractedAtBetween(LocalDateTime start, LocalDateTime end);
}
