package com.rakesh.systemdesign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ==================== SECURITY CONFIG ====================
 *
 * WHY WE NEED THIS:
 *   When you add spring-boot-starter-security, Spring Security BLOCKS ALL endpoints
 *   by default — every request needs authentication. Even Swagger UI won't load!
 *
 *   So we MUST configure it to PERMIT all endpoints for now.
 *   Later (Part 8+), we'll add JWT token authentication here.
 *
 * SecurityFilterChain:
 *   This is the CHAIN of security filters every HTTP request passes through.
 *   Think of it like NestJS Guards — but more powerful.
 *
 *   Request → Filter 1 (CORS) → Filter 2 (CSRF) → Filter 3 (Auth) → Controller
 *
 *   We're configuring it to:
 *     - Disable CSRF (not needed for REST APIs — only for form-based websites)
 *     - Permit ALL requests (no authentication required for now)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())   // Disable CSRF — REST APIs don't need it
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()   // Allow ALL endpoints without auth (for now)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
