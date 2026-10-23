package com.api_rest.est.controllers;

import com.api_rest.est.domain.users.Client;
import com.api_rest.est.domain.vehicles.VehicleService;
import com.api_rest.est.domain.vehicles.dto.VehicleRequestDTO;
import com.api_rest.est.domain.vehicles.dto.VehicleResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(
            @RequestBody @Valid VehicleRequestDTO dto,
            @AuthenticationPrincipal Client authenticatedClient) {

        VehicleResponseDTO response = service.create(dto, authenticatedClient);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> listMyVehicles(
            @AuthenticationPrincipal Client authenticatedClient) {

        return ResponseEntity.ok(service.listMyVehicles(authenticatedClient));
    }
}
