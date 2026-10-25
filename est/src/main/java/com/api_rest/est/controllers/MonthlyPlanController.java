package com.api_rest.est.controllers;

import com.api_rest.est.domain.monthly.MonthlyPlanService;
import com.api_rest.est.domain.monthly.dto.MonthlyPlanRequestDTO;
import com.api_rest.est.domain.monthly.dto.MonthlyPlanResponseDTO;
import com.api_rest.est.domain.users.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monthly-plans")
public class MonthlyPlanController {

    private final MonthlyPlanService service;

    public MonthlyPlanController(MonthlyPlanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MonthlyPlanResponseDTO> contractPlan(
            @RequestBody @Valid MonthlyPlanRequestDTO dto,
            @AuthenticationPrincipal User authenticatedUser) {
        MonthlyPlanResponseDTO response = service.contractPlan(dto, authenticatedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MonthlyPlanResponseDTO>> listMyPlans(
            @AuthenticationPrincipal User authenticatedUser) {
        return ResponseEntity.ok(service.listMyPlans(authenticatedUser));
    }
}
