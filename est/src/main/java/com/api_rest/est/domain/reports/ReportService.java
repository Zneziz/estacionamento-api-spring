package com.api_rest.est.domain.reports;

import com.api_rest.est.domain.monthly.MonthlyPlan;
import com.api_rest.est.domain.monthly.MonthlyPlanRepository;
import com.api_rest.est.domain.parking.ParkingStay;
import com.api_rest.est.domain.parking.ParkingStayRepository;
import com.api_rest.est.domain.parking.ParkingStayStatus;
import com.api_rest.est.domain.reports.dto.AverageStayTimeBySlotDTO;
import com.api_rest.est.domain.reports.dto.DailyRevenueDTO;
import com.api_rest.est.domain.reports.dto.OccupancyByTypeDTO;
import com.api_rest.est.domain.reports.dto.OccupancyReportResponseDTO;
import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.spots.SpotRepository;
import com.api_rest.est.domain.spots.SpotType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportService {

    private final SpotRepository spotRepository;
    private final ParkingStayRepository stayRepository;
    private final MonthlyPlanRepository monthlyPlanRepository;

    public ReportService(
            SpotRepository spotRepository,
            ParkingStayRepository stayRepository,
            MonthlyPlanRepository monthlyPlanRepository) {
        this.spotRepository = spotRepository;
        this.stayRepository = stayRepository;
        this.monthlyPlanRepository = monthlyPlanRepository;
    }

    // RF13: Ocupação atual por tipo de vaga
    @Transactional(readOnly = true)
    public OccupancyReportResponseDTO getOccupancyReport() {
        List<Spot> allSpots = spotRepository.findAll();

        List<OccupancyByTypeDTO> byType = new ArrayList<>();
        for (SpotType type : SpotType.values()) {
            long totalForType = allSpots.stream().filter(s -> s.getType() == type).count();
            long occupiedForType = allSpots.stream().filter(s -> s.getType() == type && s.isOccupied()).count();
            long freeForType = totalForType - occupiedForType;
            double rate = totalForType > 0
                    ? BigDecimal.valueOf((double) occupiedForType * 100.0 / totalForType)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            byType.add(new OccupancyByTypeDTO(type, totalForType, occupiedForType, freeForType, rate));
        }

        long totalSpots = allSpots.size();
        long totalOccupied = allSpots.stream().filter(Spot::isOccupied).count();
        long totalFree = totalSpots - totalOccupied;
        double overallRate = totalSpots > 0
                ? BigDecimal.valueOf((double) totalOccupied * 100.0 / totalSpots)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new OccupancyReportResponseDTO(totalSpots, totalOccupied, totalFree, overallRate, byType);
    }

    // RF14: Faturamento por dia, separado em avulso, mensalista e convênio
    @Transactional(readOnly = true)
    public List<DailyRevenueDTO> getDailyRevenueReport(LocalDate startDate, LocalDate endDate) {
        List<ParkingStay> closedStays = stayRepository.findAllByStatus(ParkingStayStatus.CLOSED);
        List<MonthlyPlan> allPlans = monthlyPlanRepository.findAll();

        Map<LocalDate, BigDecimal[]> revenueMap = new TreeMap<>();

        for (ParkingStay stay : closedStays) {
            if (!stay.isPaid() || stay.getTotalAmount() == null) {
                continue;
            }
            LocalDate date = stay.getPaidAt() != null
                    ? stay.getPaidAt().toLocalDate()
                    : stay.getExitTime().toLocalDate();

            if (!isWithinRange(date, startDate, endDate)) {
                continue;
            }

            BigDecimal[] buckets = revenueMap.computeIfAbsent(date, d -> new BigDecimal[]{
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            });

            BigDecimal amount = stay.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
            if (stay.getPartnerStore() != null) {
                // Faturamento de convênio
                buckets[2] = buckets[2].add(amount);
            } else if (stay.isMonthlyPlanApplied()) {
                // Faturamento de mensalista (ex: cobrança extra de ticket perdido em permanência de mensalista)
                buckets[1] = buckets[1].add(amount);
            } else {
                // Faturamento avulso (inclui mensalista com mensalidade vencida que paga como avulso)
                buckets[0] = buckets[0].add(amount);
            }
        }

        for (MonthlyPlan plan : allPlans) {
            if (plan.getContractedAt() == null || plan.getMonthlyFee() == null) {
                continue;
            }
            LocalDate date = plan.getContractedAt().toLocalDate();
            if (!isWithinRange(date, startDate, endDate)) {
                continue;
            }

            BigDecimal[] buckets = revenueMap.computeIfAbsent(date, d -> new BigDecimal[]{
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            });

            buckets[1] = buckets[1].add(plan.getMonthlyFee().setScale(2, RoundingMode.HALF_UP));
        }

        List<DailyRevenueDTO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal[]> entry : revenueMap.entrySet()) {
            BigDecimal casual = entry.getValue()[0];
            BigDecimal monthly = entry.getValue()[1];
            BigDecimal partner = entry.getValue()[2];
            BigDecimal total = casual.add(monthly).add(partner).setScale(2, RoundingMode.HALF_UP);
            result.add(new DailyRevenueDTO(entry.getKey(), casual, monthly, partner, total));
        }

        return result;
    }

    // RF15: Tempo médio de permanência por faixa de horário de entrada
    @Transactional(readOnly = true)
    public List<AverageStayTimeBySlotDTO> getAverageStayTimeReport() {
        List<ParkingStay> closedStays = stayRepository.findAllByStatus(ParkingStayStatus.CLOSED);

        int[][] slots = {
                {0, 5},
                {6, 11},
                {12, 17},
                {18, 23}
        };
        String[] labels = {
                "00:00 - 05:59 (Madrugada)",
                "06:00 - 11:59 (Manhã)",
                "12:00 - 17:59 (Tarde)",
                "18:00 - 23:59 (Noite)"
        };

        List<AverageStayTimeBySlotDTO> report = new ArrayList<>();

        for (int i = 0; i < slots.length; i++) {
            int startHour = slots[i][0];
            int endHour = slots[i][1];

            List<Long> durations = closedStays.stream()
                    .filter(s -> s.getEntryTime() != null && s.getExitTime() != null)
                    .filter(s -> {
                        int hour = s.getEntryTime().getHour();
                        return hour >= startHour && hour <= endHour;
                    })
                    .map(s -> Duration.between(s.getEntryTime(), s.getExitTime()).toMinutes())
                    .toList();

            long count = durations.size();
            double avgMinutes = 0.0;
            if (count > 0) {
                double rawAvg = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
                avgMinutes = BigDecimal.valueOf(rawAvg).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }

            report.add(new AverageStayTimeBySlotDTO(labels[i], startHour, endHour, count, avgMinutes));
        }

        return report;
    }

    private boolean isWithinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }
}
