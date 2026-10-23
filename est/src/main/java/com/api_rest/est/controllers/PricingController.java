package com.api_rest.est.controllers;

import com.api_rest.est.domain.pricing.PricingService;
import com.api_rest.est.domain.pricing.dto.PricingRequestDTO;
import com.api_rest.est.domain.pricing.dto.PricingResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pricing")
public class PricingController {

    private final PricingService service;

    public PricingController(PricingService service) {
        this.service = service;
    }

    @PutMapping
    public ResponseEntity<PricingResponseDTO> saveOrUpdate(@RequestBody @Valid PricingRequestDTO dto) {
        PricingResponseDTO response = service.saveOrUpdate(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PricingResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }
}
