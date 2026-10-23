package com.api_rest.est.controllers;

import com.api_rest.est.domain.parking.ParkingStayService;
import com.api_rest.est.domain.parking.dto.EntryRequestDTO;
import com.api_rest.est.domain.parking.dto.ParkingStayResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stays")
public class ParkingStayController {

    private final ParkingStayService service;

    public ParkingStayController(ParkingStayService service) {
        this.service = service;
    }

    @PostMapping("/entry")
    public ResponseEntity<ParkingStayResponseDTO> registerEntry(@RequestBody @Valid EntryRequestDTO dto) {
        ParkingStayResponseDTO response = service.registerEntry(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
