package com.api_rest.est.controllers;

import com.api_rest.est.domain.partners.PartnerStoreService;
import com.api_rest.est.domain.partners.dto.PartnerStoreRequestDTO;
import com.api_rest.est.domain.partners.dto.PartnerStoreResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partners")
public class PartnerStoreController {

    private final PartnerStoreService service;

    public PartnerStoreController(PartnerStoreService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PartnerStoreResponseDTO> create(@RequestBody @Valid PartnerStoreRequestDTO dto) {
        PartnerStoreResponseDTO response = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PartnerStoreResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }
}
