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
        createUserIfNotExists("admin@estacionamento.com", "admin1234", Role.ADMIN);
        createUserIfNotExists("operador@estacionamento.com", "operador1234", Role.OPERATOR);
        createUserIfNotExists("admin@parking.com", "admin1234", Role.ADMIN);
        createUserIfNotExists("operator@parking.com", "operator1234", Role.OPERATOR);

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

    private void createUserIfNotExists(String email, String rawPassword, Role role) {
        if (userRepository.findByEmail(email) == null) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            userRepository.save(user);
        }
    }
}
