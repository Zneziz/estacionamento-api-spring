package com.api_rest.est.domain.vehicles;
import com.api_rest.est.domain.users.Client;
import com.api_rest.est.domain.users.ClientRepository;
import com.api_rest.est.domain.vehicles.dto.VehicleRequestDTO;
import com.api_rest.est.domain.vehicles.dto.VehicleResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    public VehicleService(VehicleRepository vehicleRepository, ClientRepository clientRepository) {
        this.vehicleRepository = vehicleRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public VehicleResponseDTO create(VehicleRequestDTO dto, Client authenticatedClient) {
        String normalizedPlate = dto.licensePlate().trim().toUpperCase();

        if (vehicleRepository.existsByLicensePlateIgnoreCase(normalizedPlate)) {
            throw new IllegalArgumentException("Vehicle with this license plate already exists.");
        }

        // Busca o cliente gerenciado pelo banco
        Client client = clientRepository.findById(authenticatedClient.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found."));

        // RF05: Se o cliente informou a credencial PCD, atualizamos o cadastro dele
        if (dto.pcdCredential() != null && !dto.pcdCredential().isBlank()) {
            client.setPcdCredential(dto.pcdCredential().trim());
            clientRepository.save(client);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(normalizedPlate);
        vehicle.setType(dto.type());
        vehicle.setElectric(dto.electric());
        vehicle.setClient(client);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return new VehicleResponseDTO(savedVehicle);
    }

    public List<VehicleResponseDTO> listMyVehicles(Client authenticatedClient) {
        // Regra geral: O cliente só acessa os próprios veículos
        return vehicleRepository.findAllByClientId(authenticatedClient.getId())
                .stream()
                .map(VehicleResponseDTO::new)
                .toList();
    }
}
