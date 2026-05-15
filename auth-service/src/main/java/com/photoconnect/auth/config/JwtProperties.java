package com.photoconnect.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Strongly-typed binding for {@code auth.jwt.*} properties.
 *
 * <p>Records + {@code @ConfigurationProperties} are the modern Spring Boot
 * pattern: immutable, no setters, no Lombok needed, and validated at startup
 * by {@code @Validated}. Missing or invalid values fail the app before it
 * starts serving traffic.</p>
 *
 * @param issuer            value placed in the JWT {@code iss} claim
 * @param audience          value placed in the JWT {@code aud} claim
 * @param accessTokenTtl    access token lifetime (e.g. PT15M)
 * @param refreshTokenTtl   refresh token lifetime (e.g. P7D)
 * @param privateKeyPath    filesystem path to the PEM-encoded RSA private key
 * @param publicKeyPath     filesystem path to the PEM-encoded RSA public key
 */
@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull  Duration accessTokenTtl,
        @NotNull  Duration refreshTokenTtl,
        @NotBlank String privateKeyPath,
        @NotBlank String publicKeyPath
) {}
