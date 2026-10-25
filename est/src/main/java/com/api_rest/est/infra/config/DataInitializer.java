package com.api_rest.est.infra.config;

import com.api_rest.est.domain.pricing.PricingRepository;
import com.api_rest.est.domain.pricing.PricingTable;
import com.api_rest.est.domain.users.Role;
import com.api_rest.est.domain.users.User;
import com.api_rest.est.domain.users.UserRepository;
import com.api_rest.est.domain.vehicles.VehicleType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PricingRepository pricingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            PricingRepository pricingRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.pricingRepository = pricingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Carga inicial: contas de Administrador e Operador já vêm criadas
        if (userRepository.findByEmail("admin@estacionamento.com") == null) {
            User admin = new User();
            admin.setEmail("admin@estacionamento.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        if (userRepository.findByEmail("operador@estacionamento.com") == null) {
            User operator = new User();
            operator.setEmail("operador@estacionamento.com");
            operator.setPassword(passwordEncoder.encode("operador1234"));
            operator.setRole(Role.OPERATOR);
            userRepository.save(operator);
        }

        // Tabela de preços padrão caso ainda não exista
        if (pricingRepository.findByVehicleType(VehicleType.CAR).isEmpty()) {
            PricingTable carPricing = new PricingTable();
            carPricing.setVehicleType(VehicleType.CAR);
            carPricing.setFirstHourRate(new BigDecimal("10.00"));
            carPricing.setAdditionalFractionRate(new BigDecimal("3.00"));
            carPricing.setDailyCap(new BigDecimal("50.00"));
            pricingRepository.save(carPricing);
        }

        if (pricingRepository.findByVehicleType(VehicleType.MOTORCYCLE).isEmpty()) {
            PricingTable motoPricing = new PricingTable();
            motoPricing.setVehicleType(VehicleType.MOTORCYCLE);
            motoPricing.setFirstHourRate(new BigDecimal("5.00"));
            motoPricing.setAdditionalFractionRate(new BigDecimal("1.50"));
            motoPricing.setDailyCap(new BigDecimal("25.00"));
            pricingRepository.save(motoPricing);
        }
    }
}
