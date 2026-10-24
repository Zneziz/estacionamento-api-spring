package com.api_rest.est.domain.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class FeeCalculator {

    private static final int MINUTES_IN_HOUR = 60;
    private static final int FRACTION_MINUTES = 15;
    private static final int MINUTES_IN_DAY = 24 * 60; // 1440 minutos

    public BigDecimal calculateFee(LocalDateTime entryTime, LocalDateTime exitTime, PricingTable pricing) {
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();

        // RF04 / RF08: Os 10 primeiros minutos são de tolerância, sem cobrança
        if (totalMinutes <= PricingTable.TOLERANCE_MINUTES) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Quantos períodos completos de 24 horas se passaram
        long fullDays = totalMinutes / MINUTES_IN_DAY;
        long remainingMinutes = totalMinutes % MINUTES_IN_DAY;

        // Multiplica os dias completos pelo teto diário
        BigDecimal totalAmount = pricing.getDailyCap().multiply(BigDecimal.valueOf(fullDays));

        // Calcula o valor do período parcial restante (se houver)
        if (remainingMinutes > 0) {
            BigDecimal partialDayAmount = calculatePeriodAmount(remainingMinutes, pricing);
            totalAmount = totalAmount.add(partialDayAmount);
        }

        return totalAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePeriodAmount(long minutes, PricingTable pricing) {
        BigDecimal amount = pricing.getFirstHourRate();

        if (minutes > MINUTES_IN_HOUR) {
            long extraMinutes = minutes - MINUTES_IN_HOUR;
            // Arredonda para cima a cada fração de 15 minutos (ex: 1 a 15 min = 1 fração; 16 a 30 min = 2 frações)
            long fractions = (long) Math.ceil((double) extraMinutes / FRACTION_MINUTES);

            BigDecimal fractionsCost = pricing.getAdditionalFractionRate()
                    .multiply(BigDecimal.valueOf(fractions));
            amount = amount.add(fractionsCost);
        }

        // Aplica o teto diário caso a soma da 1ª hora + frações ultrapasse a diária neste período de 24h
        if (amount.compareTo(pricing.getDailyCap()) > 0) {
            return pricing.getDailyCap();
        }

        return amount;
    }
}
