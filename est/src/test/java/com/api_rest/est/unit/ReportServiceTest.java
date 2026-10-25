package com.api_rest.est.unit;

import com.api_rest.est.domain.monthly.MonthlyPlan;
import com.api_rest.est.domain.monthly.MonthlyPlanRepository;
import com.api_rest.est.domain.parking.ParkingStay;
import com.api_rest.est.domain.parking.ParkingStayRepository;
import com.api_rest.est.domain.parking.ParkingStayStatus;
import com.api_rest.est.domain.partners.PartnerStore;
import com.api_rest.est.domain.reports.ReportService;
import com.api_rest.est.domain.reports.dto.AverageStayTimeBySlotDTO;
import com.api_rest.est.domain.reports.dto.DailyRevenueDTO;
import com.api_rest.est.domain.reports.dto.OccupancyReportResponseDTO;
import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.spots.SpotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private SpotRepository spotRepository;
    @Mock
    private ParkingStayRepository stayRepository;
    @Mock
    private MonthlyPlanRepository monthlyPlanRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("RF13: Deve calcular ocupação atual por tipo de vaga")
    void shouldCalculateOccupancyBySpotType() {
        List<Spot> spots = List.of(
                new Spot(1L, "C-01", SpotType.CAR, true),
                new Spot(2L, "C-02", SpotType.CAR, false),
                new Spot(3L, "M-01", SpotType.MOTORCYCLE, true),
                new Spot(4L, "P-01", SpotType.PCD, false),
                new Spot(5L, "E-01", SpotType.ELECTRIC, false)
        );
        when(spotRepository.findAll()).thenReturn(spots);

        OccupancyReportResponseDTO report = reportService.getOccupancyReport();

        assertEquals(5, report.totalSpots());
        assertEquals(2, report.totalOccupied());
        assertEquals(3, report.totalFree());
        assertEquals(40.0, report.overallOccupancyRatePercentage());
    }

    @Test
    @DisplayName("RF14: Deve separar faturamento diário em avulso, mensalista e convênio")
    void shouldCalculateDailyRevenueByCategory() {
        LocalDate today = LocalDate.of(2026, 10, 24);
        LocalDateTime exitTime = today.atTime(14, 0);

        // Avulso: R$ 20.00
        ParkingStay casualStay = new ParkingStay();
        casualStay.setPaid(true);
        casualStay.setPaidAt(exitTime);
        casualStay.setExitTime(exitTime);
        casualStay.setTotalAmount(new BigDecimal("20.00"));
        casualStay.setStatus(ParkingStayStatus.CLOSED);

        // Convênio: R$ 10.00
        ParkingStay partnerStay = new ParkingStay();
        partnerStay.setPaid(true);
        partnerStay.setPaidAt(exitTime);
        partnerStay.setExitTime(exitTime);
        partnerStay.setTotalAmount(new BigDecimal("10.00"));
        partnerStay.setPartnerStore(new PartnerStore(1L, "Loja A", 1, true));
        partnerStay.setStatus(ParkingStayStatus.CLOSED);

        // Mensalista (contratação do plano no dia): R$ 250.00
        MonthlyPlan monthlyPlan = new MonthlyPlan();
        monthlyPlan.setMonthlyFee(new BigDecimal("250.00"));
        monthlyPlan.setContractedAt(exitTime);

        when(stayRepository.findAllByStatus(ParkingStayStatus.CLOSED))
                .thenReturn(List.of(casualStay, partnerStay));
        when(monthlyPlanRepository.findAll()).thenReturn(List.of(monthlyPlan));

        List<DailyRevenueDTO> report = reportService.getDailyRevenueReport(today, today);

        assertEquals(1, report.size());
        DailyRevenueDTO dayReport = report.get(0);
        assertEquals(new BigDecimal("20.00"), dayReport.casualRevenue());
        assertEquals(new BigDecimal("250.00"), dayReport.monthlyRevenue());
        assertEquals(new BigDecimal("10.00"), dayReport.partnerRevenue());
        assertEquals(new BigDecimal("280.00"), dayReport.totalRevenue());
    }

    @Test
    @DisplayName("RF15: Deve calcular tempo médio de permanência por faixa de horário de entrada")
    void shouldCalculateAverageStayTimeByEntrySlot() {
        ParkingStay morningStay1 = new ParkingStay();
        morningStay1.setEntryTime(LocalDateTime.of(2026, 10, 24, 8, 0));
        morningStay1.setExitTime(LocalDateTime.of(2026, 10, 24, 9, 0)); // 60 min
        morningStay1.setStatus(ParkingStayStatus.CLOSED);

        ParkingStay morningStay2 = new ParkingStay();
        morningStay2.setEntryTime(LocalDateTime.of(2026, 10, 24, 10, 0));
        morningStay2.setExitTime(LocalDateTime.of(2026, 10, 24, 12, 0)); // 120 min
        morningStay2.setStatus(ParkingStayStatus.CLOSED);

        when(stayRepository.findAllByStatus(ParkingStayStatus.CLOSED))
                .thenReturn(List.of(morningStay1, morningStay2));

        List<AverageStayTimeBySlotDTO> report = reportService.getAverageStayTimeReport();

        assertEquals(4, report.size());
        // Faixa 06:00 - 11:59 (índice 1) deve ter 2 permanências e média de 90.0 minutos
        assertEquals(2, report.get(1).stayCount());
        assertEquals(90.0, report.get(1).averageDurationMinutes());
    }
}
