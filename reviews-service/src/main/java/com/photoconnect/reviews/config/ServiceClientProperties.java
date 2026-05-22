package com.photoconnect.reviews.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Binding for {@code reviews.service-client.*} — credentials and tunables for
 * the service-to-service token client that authenticates outbound calls to
 * other PhotoConnect services.
 *
 * <p>The cleartext {@code clientSecret} lives in config-repo today. In Phase 2
 * it moves to AWS Secrets Manager and is read at startup; the API of this
 * record doesn't change.</p>
 */
@Validated
@ConfigurationProperties(prefix = "reviews.service-client")
public record ServiceClientProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String authServiceUrl,
        @NotNull  Duration refreshSkew
) {}
