package com.api_rest.est.domain.vehicles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    List<Vehicle> findAllByClientId(Long clientId);

    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate); // Será muito útil no RF06!
}
