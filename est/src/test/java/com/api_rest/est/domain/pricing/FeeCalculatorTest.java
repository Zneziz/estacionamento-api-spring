package com.api_rest.est.domain.pricing;

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
                new BigDecimal("10.00"), // 1ª hora: R$ 10,00
                new BigDecimal("3.00"),  // Fração 15 min: R$ 3,00
                new BigDecimal("40.00")  // Teto diário: R$ 40,00
        );
    }

    @Test
    @DisplayName("RF04/RF08: Até 10 minutos de tolerância não deve cobrar nada")
    void shouldReturnZeroWithinTolerance() {
        LocalDateTime entry = LocalDateTime.now();
        LocalDateTime exit = entry.plusMinutes(10);

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing);
        assertEquals(new BigDecimal("0.00"), fee);
    }

    @Test
    @DisplayName("RF04/RF08: Entre 11 e 60 minutos deve cobrar exatamente a primeira hora")
    void shouldChargeFirstHourRate() {
        LocalDateTime entry = LocalDateTime.now();
        LocalDateTime exit = entry.plusMinutes(45);

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing);
        assertEquals(new BigDecimal("10.00"), fee);
    }

    @Test
    @DisplayName("RF04/RF08: 1h e 20min deve cobrar 1ª hora + 2 frações de 15 minutos")
    void shouldChargeFirstHourPlusFractions() {
        LocalDateTime entry = LocalDateTime.now();
        LocalDateTime exit = entry.plusMinutes(80); // 60 min + 20 min (2 frações)

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing);
        // 10.00 + (2 * 3.00) = 16.00
        assertEquals(new BigDecimal("16.00"), fee);
    }

    @Test
    @DisplayName("RF04/RF08: Deve limitar ao teto diário dentro de 24 horas")
    void shouldApplyDailyCapWithin24Hours() {
        LocalDateTime entry = LocalDateTime.now();
        LocalDateTime exit = entry.plusHours(10); // Daria R$ 118,00 sem teto

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing);
        assertEquals(new BigDecimal("40.00"), fee);
    }

    @Test
    @DisplayName("RF08: Deve aplicar o teto diário a cada período de 24h + horas restantes")
    void shouldApplyDailyCapPer24HourPeriod() {
        LocalDateTime entry = LocalDateTime.now();
        LocalDateTime exit = entry.plusHours(25); // 1 dia completo (40.00) + 1 hora (10.00)

        BigDecimal fee = calculator.calculateFee(entry, exit, carPricing);
        assertEquals(new BigDecimal("50.00"), fee);
    }
}
