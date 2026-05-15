package com.photoconnect.auth;

import com.photoconnect.auth.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * PhotoConnect — auth-service entry point.
 *
 * <p>Owner of the {@code auth_db} schema. Issues RS256-signed JWT access +
 * refresh tokens, stores users with BCrypt-12 password hashes, and blacklists
 * revoked tokens in Redis until they expire.</p>
 *
 * <p>{@code @EnableConfigurationProperties(JwtProperties.class)} binds our
 * {@code auth.jwt.*} settings (issuer, audience, TTLs, key paths) so they
 * can be injected as a single record into services.</p>
 *
 * <p>{@code @EnableJpaAuditing} lives on {@code AuditConfig} (not here) so
 * non-JPA test slices like {@code @WebMvcTest} can run without dragging in
 * JPA machinery.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
