package com.api_rest.est.controllers;

import com.api_rest.est.domain.reports.ReportService;
import com.api_rest.est.domain.reports.dto.AverageStayTimeBySlotDTO;
import com.api_rest.est.domain.reports.dto.DailyRevenueDTO;
import com.api_rest.est.domain.reports.dto.OccupancyReportResponseDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // RF13: Ocupação atual por tipo de vaga
    @GetMapping("/occupancy")
    public ResponseEntity<OccupancyReportResponseDTO> getOccupancyReport() {
        return ResponseEntity.ok(reportService.getOccupancyReport());
    }

    // RF14: Faturamento por dia, separado em avulso, mensalista e convênio
    @GetMapping("/revenue")
    public ResponseEntity<List<DailyRevenueDTO>> getDailyRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getDailyRevenueReport(startDate, endDate));
    }

    // RF15: Tempo médio de permanência por faixa de horário de entrada
    @GetMapping("/average-stay-time")
    public ResponseEntity<List<AverageStayTimeBySlotDTO>> getAverageStayTimeReport() {
        return ResponseEntity.ok(reportService.getAverageStayTimeReport());
    }
}
