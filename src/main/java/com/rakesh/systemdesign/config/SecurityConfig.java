package com.rakesh.systemdesign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ==================== @Configuration CLASS ====================
 *
 * WHAT:  A class marked @Configuration is a place to define BEANS manually.
 *        Normally Spring auto-creates beans by scanning @Service, @Repository, etc.
 *        But some classes (like BCryptPasswordEncoder) are from EXTERNAL LIBRARIES —
 *        you can't add @Service to their code. So you create them HERE.
 *
 * WHAT IS @Bean?
 *   A method marked @Bean tells Spring:
 *     "Call this method at startup → take the returned object → store it in the IoC container"
 *
 *   After this, ANYWHERE in your code you can inject PasswordEncoder:
 *     public AuthService(PasswordEncoder passwordEncoder) { ... }
 *     Spring will inject the BCryptPasswordEncoder object created here.
 *
 * NestJS comparison:
 *   In NestJS you'd register in a module:
 *     @Module({
 *       providers: [{ provide: 'PasswordEncoder', useFactory: () => new BcryptService() }]
 *     })
 *
 * ==================== WHAT IS BCrypt? ====================
 *
 *   BCrypt is a PASSWORD HASHING algorithm. It converts plain text to an irreversible hash.
 *
 *   Input:  "myPassword123"
 *   Output: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
 *
 *   Key properties:
 *     1. ONE-WAY: You CANNOT convert hash back to password. No decryption possible.
 *     2. SALTED: Same password → DIFFERENT hash each time (random salt added).
 *        "myPassword123" → "$2a$10$abc..." (first time)
 *        "myPassword123" → "$2a$10$xyz..." (second time)
 *        Hackers can't use precomputed tables (rainbow tables) to crack it.
 *     3. SLOW ON PURPOSE: Takes ~100ms to hash. Fast enough for login, too slow for brute force.
 *
 *   How login verification works:
 *     encoder.matches("plainPassword", "$2a$10$storedHash...")
 *     → BCrypt extracts the salt from the stored hash
 *     → Hashes "plainPassword" with that same salt
 *     → Compares: do they match? → true/false
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // This object is now a BEAN in the IoC container.
        // Any class can inject it via constructor: PasswordEncoder passwordEncoder
    }
}
