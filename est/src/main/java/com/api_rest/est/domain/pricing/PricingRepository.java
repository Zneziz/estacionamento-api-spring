package com.api_rest.est.domain.pricing;

import com.api_rest.est.domain.vehicles.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PricingRepository extends JpaRepository<PricingTable, Long> {
    Optional<PricingTable> findByVehicleType(VehicleType vehicleType);
}
