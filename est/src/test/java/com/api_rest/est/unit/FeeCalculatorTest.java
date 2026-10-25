package com.api_rest.est.unit;

import com.api_rest.est.domain.pricing.FeeCalculator;
import com.api_rest.est.domain.pricing.PricingTable;
import com.api_rest.est.domain.vehicles.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeCalculatorTest {

    private FeeCalculator calculator;
    private PricingTable carPricing;

    @BeforeEach
    void setUp() {
        calculator = new FeeCalculator();
        carPricing = new PricingTable(
                1L,
                VehicleType.CAR,
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("50.00")
        );
    }

    @Test
    @DisplayName("RF04/RF08: Deve cobrar R$ 0.00 dentro dos 10 primeiros minutos de tolerância")
    void shouldChargeZeroWithinTolerance() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);
        LocalDateTime exit = entry.plusMinutes(10);

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing);

        assertEquals(new BigDecimal("0.00"), fee);
    }

    @Test
    @DisplayName("RF04/RF08: Deve cobrar o valor da primeira hora entre 11 e 60 minutos")
    void shouldChargeFirstHourRateUpTo60Minutes() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);

        assertEquals(new BigDecimal("10.00"), calculator.calculateFee(entry, entry.plusMinutes(11), carPricing));
        assertEquals(new BigDecimal("10.00"), calculator.calculateFee(entry, entry.plusMinutes(60), carPricing));
    }

    @Test
    @DisplayName("RF04/RF08: Deve cobrar frações adicionais de 15 minutos após a primeira hora")
    void shouldChargeAdditionalFractionsOf15Minutes() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);

        // 61 min = 1ª hora (10.00) + 1 fração (3.00) = 13.00
        assertEquals(new BigDecimal("13.00"), calculator.calculateFee(entry, entry.plusMinutes(61), carPricing));
        // 75 min = 1ª hora (10.00) + 1 fração (3.00) = 13.00
        assertEquals(new BigDecimal("13.00"), calculator.calculateFee(entry, entry.plusMinutes(75), carPricing));
        // 76 min = 1ª hora (10.00) + 2 frações (6.00) = 16.00
        assertEquals(new BigDecimal("16.00"), calculator.calculateFee(entry, entry.plusMinutes(76), carPricing));
    }

    @Test
    @DisplayName("RF04/RF08: Deve aplicar o teto diário dentro de um período de 24h")
    void shouldApplyDailyCapWithin24Hours() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);
        // 10 horas = 10.00 + (36 frações * 3.00 = 108.00) = 118.00 -> limitado a 50.00
        LocalDateTime exit = entry.plusHours(10);

        assertEquals(new BigDecimal("50.00"), calculator.calculateFee(entry, exit, carPricing));
    }

    @Test
    @DisplayName("RF08: Deve aplicar o teto diário a cada período de 24 horas mais o período parcial")
    void shouldCalculateMultipleDaysPlusPartialPeriod() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);
        // 24h + 30min = 1 diária (50.00) + 1ª hora do 2º dia (10.00) = 60.00
        LocalDateTime exit = entry.plusHours(24).plusMinutes(30);

        assertEquals(new BigDecimal("60.00"), calculator.calculateFee(entry, exit, carPricing));
    }

    @Test
    @DisplayName("RF09: Saída sem ticket (ticket perdido) deve cobrar uma diária a mais sobre o valor calculado")
    void shouldAddExtraDailyCapWhenTicketIsLost() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);
        LocalDateTime exit = entry.plusMinutes(45); // 1ª hora = 10.00 + diária extra (50.00) = 60.00

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing, 0, false, true);

        assertEquals(new BigDecimal("60.00"), fee);
    }

    @Test
    @DisplayName("RF10: Mensalista ativo em dia não paga na saída, mas paga uma diária se perder o ticket")
    void shouldChargeZeroForActiveMonthlyMemberUnlessTicketLost() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);
        LocalDateTime exit = entry.plusHours(3);

        // Mensalista em dia sem perda de ticket -> 0.00
        assertEquals(new BigDecimal("0.00"), calculator.calculateFee(entry, exit, carPricing, 0, true, false));

        // Mensalista em dia com perda de ticket -> 0.00 + 50.00 = 50.00
        assertEquals(new BigDecimal("50.00"), calculator.calculateFee(entry, exit, carPricing, 0, true, true));
    }

    @Test
    @DisplayName("RF11: Convênio deve abater horas antes do cálculo do valor")
    void shouldDeductPartnerDiscountHoursBeforeCalculation() {
        LocalDateTime entry = LocalDateTime.of(2026, 10, 24, 10, 0);

        // 2 horas de permanência com 2 horas de desconto do convênio -> 0 minutos cobráveis -> R$ 0.00
        assertEquals(new BigDecimal("0.00"),
                calculator.calculateFee(entry, entry.plusHours(2), carPricing, 2, false, false));

        // 2h30 (150 min) de permanência com 2h (120 min) de desconto -> 30 min restantes -> 1ª hora (R$ 10.00)
        assertEquals(new BigDecimal("10.00"),
                calculator.calculateFee(entry, entry.plusMinutes(150), carPricing, 2, false, false));

        // 3h15 (195 min) de permanência com 2h (120 min) de desconto -> 75 min restantes -> 1ª hora + 1 fração (R$ 13.00)
        assertEquals(new BigDecimal("13.00"),
                calculator.calculateFee(entry, entry.plusMinutes(195), carPricing, 2, false, false));
    }
}
