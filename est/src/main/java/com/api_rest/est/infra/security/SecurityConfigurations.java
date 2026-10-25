package com.api_rest.est.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // Rotas públicas (não precisam de token)
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/clients").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();

                    // Rotas restritas ao Administrador (RF03, RF04, RF11, RF13, RF14, RF15)
                    req.requestMatchers(HttpMethod.POST, "/spots").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.PUT, "/pricing").hasRole("ADMIN");
                    req.requestMatchers(HttpMethod.POST, "/partners").hasRole("ADMIN");
                    req.requestMatchers("/reports/**").hasRole("ADMIN");

                    // Rotas restritas ao Cliente (RF05, RF10, RF12)
                    req.requestMatchers("/vehicles/**").hasRole("CLIENT");
                    req.requestMatchers("/monthly-plans/**").hasRole("CLIENT");
                    req.requestMatchers(HttpMethod.GET, "/stays/history").hasRole("CLIENT");

                    // Rotas de Operação (RF06, RF08, RF09, RF11)
                    req.requestMatchers(HttpMethod.POST, "/stays/entry").hasAnyRole("OPERATOR", "ADMIN");
                    req.requestMatchers(HttpMethod.POST, "/stays/exit").hasAnyRole("OPERATOR", "ADMIN");

                    // Qualquer outra requisição precisa estar autenticada
                    req.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
