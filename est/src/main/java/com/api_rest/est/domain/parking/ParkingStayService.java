package com.api_rest.est.domain.parking;

import com.api_rest.est.domain.parking.dto.EntryRequestDTO;
import com.api_rest.est.domain.parking.dto.ParkingStayResponseDTO;
import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.spots.SpotType;
import com.api_rest.est.domain.vehicles.Vehicle;
import com.api_rest.est.domain.vehicles.VehicleRepository;
import com.api_rest.est.domain.vehicles.VehicleType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParkingStayService {

    private final ParkingStayRepository stayRepository;
    private final VehicleRepository vehicleRepository;
    private final SpotRepository spotRepository;

    public ParkingStayService(
            ParkingStayRepository stayRepository,
            VehicleRepository vehicleRepository,
            SpotRepository spotRepository) {
        this.stayRepository = stayRepository;
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
    }

    @Transactional
    public ParkingStayResponseDTO registerEntry(EntryRequestDTO dto) {
        String plate = dto.licensePlate().trim().toUpperCase();

        // Regra RF06: A mesma placa não pode ter duas permanências abertas
        if (stayRepository.existsByVehicleLicensePlateIgnoreCaseAndStatus(plate, ParkingStayStatus.OPEN)) {
            throw new IllegalArgumentException("This vehicle already has an open parking stay.");
        }

        // Busca o veículo pela placa
        Vehicle vehicle = vehicleRepository.findByLicensePlateIgnoreCase(plate)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with license plate: " + plate));

        // Determina os tipos de vaga compatíveis com o veículo e o cliente
        List<SpotType> compatibleTypes = getCompatibleSpotTypes(vehicle);

        // Busca a primeira vaga livre compatível; se não houver, recusa a entrada
        Spot allocatedSpot = spotRepository.findFirstByOccupiedFalseAndTypeIn(compatibleTypes)
                .orElseThrow(() -> new IllegalArgumentException("No compatible free spot available. Entry refused."));

        // Ocupa a vaga física
        allocatedSpot.setOccupied(true);
        spotRepository.save(allocatedSpot);

        // Registra a permanência aberta
        ParkingStay stay = new ParkingStay();
        stay.setVehicle(vehicle);
        stay.setSpot(allocatedSpot);
        stay.setEntryTime(LocalDateTime.now());
        stay.setStatus(ParkingStayStatus.OPEN);

        ParkingStay savedStay = stayRepository.save(stay);
        return new ParkingStayResponseDTO(savedStay);
    }

    private List<SpotType> getCompatibleSpotTypes(Vehicle vehicle) {
        List<SpotType> types = new ArrayList<>();

        if (vehicle.getType() == VehicleType.MOTORCYCLE) {
            if (vehicle.isElectric()) {
                types.add(SpotType.ELECTRIC); // Vaga de elétrico só para veículo elétrico
            }
            types.add(SpotType.MOTORCYCLE);
        } else if (vehicle.getType() == VehicleType.CAR) {
            // Vaga PCD só para veículo de cliente com credencial
            String pcdCredential = vehicle.getClient().getPcdCredential();
            if (pcdCredential != null && !pcdCredential.isBlank()) {
                types.add(SpotType.PCD);
            }
            // Vaga de elétrico só para veículo elétrico
            if (vehicle.isElectric()) {
                types.add(SpotType.ELECTRIC);
            }
            types.add(SpotType.CAR);
        }

        return types;
    }
}
