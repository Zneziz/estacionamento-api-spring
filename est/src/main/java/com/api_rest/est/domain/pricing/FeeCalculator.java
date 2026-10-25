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
        return calculateFee(entryTime, exitTime, pricing, 0, false, false);
    }

    public BigDecimal calculateFee(
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            PricingTable pricing,
            int discountHours,
            boolean isActiveMonthlyMember,
            boolean lostTicket) {

        BigDecimal baseAmount;

        // RF10: Mensalista com mensalidade em dia não paga pela permanência na saída
        if (isActiveMonthlyMember) {
            baseAmount = BigDecimal.ZERO;
        } else {
            long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();

            // RF04 / RF08: Os 10 primeiros minutos são de tolerância, sem cobrança
            if (totalMinutes <= PricingTable.TOLERANCE_MINUTES) {
                baseAmount = BigDecimal.ZERO;
            } else {
                // RF11: Na saída, o operador informa o convênio e as horas são abatidas antes do cálculo
                long discountMinutes = Math.max(0, discountHours) * 60L;
                long billableMinutes = totalMinutes - discountMinutes;

                if (billableMinutes <= 0) {
                    baseAmount = BigDecimal.ZERO;
                } else {
                    // Quantos períodos completos de 24 horas se passaram sobre o tempo cobrável
                    long fullDays = billableMinutes / MINUTES_IN_DAY;
                    long remainingMinutes = billableMinutes % MINUTES_IN_DAY;

                    // Multiplica os dias completos pelo teto diário
                    baseAmount = pricing.getDailyCap().multiply(BigDecimal.valueOf(fullDays));

                    // Calcula o valor do período parcial restante (se houver)
                    if (remainingMinutes > 0) {
                        BigDecimal partialDayAmount = calculatePeriodAmount(remainingMinutes, pricing);
                        baseAmount = baseAmount.add(partialDayAmount);
                    }
                }
            }
        }

        // RF09: Ticket perdido cobra uma diária a mais sobre o valor calculado
        if (lostTicket) {
            baseAmount = baseAmount.add(pricing.getDailyCap());
        }

        return baseAmount.setScale(2, RoundingMode.HALF_UP);
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
