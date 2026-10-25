package com.api_rest.est.infra;

import com.api_rest.est.domain.users.Role;
import com.api_rest.est.domain.users.User;
import com.api_rest.est.domain.users.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@parking.com") == null) {
            User admin = new User();
            admin.setEmail("admin@parking.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        if (userRepository.findByEmail("operator@parking.com") == null) {
            User operator = new User();
            operator.setEmail("operator@parking.com");
            operator.setPassword(passwordEncoder.encode("operator1234"));
            operator.setRole(Role.OPERATOR);
            userRepository.save(operator);
        }
    }
}
