package com.api_rest.est.controllers;

import com.api_rest.est.domain.parking.ParkingStayService;
import com.api_rest.est.domain.parking.dto.*;
import com.api_rest.est.domain.users.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/exit")
    public ResponseEntity<ExitResponseDTO> registerExit(@RequestBody @Valid ExitRequestDTO dto) {
        ExitResponseDTO response = service.registerExit(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current-fee/{licensePlate}")
    public ResponseEntity<CurrentFeeResponseDTO> getCurrentFee(
            @PathVariable String licensePlate,
            @AuthenticationPrincipal User authenticatedUser) {

        CurrentFeeResponseDTO response = service.getCurrentFee(licensePlate, authenticatedUser);
        return ResponseEntity.ok(response);
    }

    // RF12: O cliente vê o histórico de permanências e os valores pagos dos próprios veículos
    @GetMapping("/history")
    public ResponseEntity<List<ParkingStayHistoryResponseDTO>> getClientHistory(
            @RequestParam(required = false) String licensePlate,
            @AuthenticationPrincipal User authenticatedUser) {

        return ResponseEntity.ok(service.getClientHistory(authenticatedUser, licensePlate));
    }
}
