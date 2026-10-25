package com.api_rest.est.controllers;

import com.api_rest.est.domain.spots.SpotService;
import com.api_rest.est.domain.spots.dto.SpotRequestDTO;
import com.api_rest.est.domain.spots.dto.SpotResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spots")
public class SpotController {

    private final SpotService service;

    public SpotController(SpotService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SpotResponseDTO> create(@RequestBody @Valid SpotRequestDTO dto) {
        SpotResponseDTO response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SpotResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }
}
