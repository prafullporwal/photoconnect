package com.photoconnect.photographer.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binding for {@code photographer.service-jwt.*} — everything we need to
 * verify a service-to-service JWT minted by auth-service.
 *
 * <p>Issuer + audience must match the values configured on auth-service,
 * otherwise the {@code requireIssuer}/{@code requireAudience} checks in
 * the parser will reject every token.</p>
 *
 * @param issuer          expected value of the {@code iss} claim
 * @param audience        expected value of the {@code aud} claim
 * @param publicKeyPath   filesystem path to the RSA public key (PEM, X.509 SPKI)
 */
@Validated
@ConfigurationProperties(prefix = "photographer.service-jwt")
public record ServiceJwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotBlank String publicKeyPath
) {}
