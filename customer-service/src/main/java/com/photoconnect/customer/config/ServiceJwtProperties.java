package com.photoconnect.customer.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binding for {@code customer.service-jwt.*} — settings needed to verify
 * service-to-service JWTs that arrive on {@code /internal/**} endpoints.
 *
 * <p>Issuer + audience must match the values configured on auth-service. The
 * public key file is the SAME file auth-service uses for signing — we only
 * need the public half here.</p>
 */
@Validated
@ConfigurationProperties(prefix = "customer.service-jwt")
public record ServiceJwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotBlank String publicKeyPath
) {}
