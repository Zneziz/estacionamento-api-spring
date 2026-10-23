package com.api_rest.est.domain.parking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParkingStayRepository extends JpaRepository<ParkingStay, Long> {

    // RF06: Verifica se a mesma placa já tem uma permanência aberta
    boolean existsByVehicleLicensePlateIgnoreCaseAndStatus(String licensePlate, ParkingStayStatus status);

    // Será usado nos próximos requisitos (RF07 e RF08) para buscar a permanência aberta
    Optional<ParkingStay> findByVehicleLicensePlateIgnoreCaseAndStatus(String licensePlate, ParkingStayStatus status);
}
