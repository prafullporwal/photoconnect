package com.photoconnect.customer.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Binding for {@code customer.service-client.*} — credentials and tunables for
 * the service-to-service token client that authenticates outbound calls to
 * other PhotoConnect services.
 *
 * <p>The cleartext {@code clientSecret} lives in config-repo today. In Phase 2
 * it moves to AWS Secrets Manager and is read at startup; the API of this
 * record doesn't change.</p>
 *
 * @param clientId         our id as a caller (e.g. {@code "customer-service"})
 * @param clientSecret     the cleartext shared secret we present to auth-service
 * @param authServiceUrl   base URL of auth-service (Eureka-discovered name with
 *                         {@code http://} prefix — Spring Cloud LoadBalancer
 *                         resolves the actual instance)
 * @param refreshSkew      mint a new token when the cached one has less than
 *                         this duration left (avoids races near expiry)
 */
@Validated
@ConfigurationProperties(prefix = "customer.service-client")
public record ServiceClientProperties(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String authServiceUrl,
        @NotNull  Duration refreshSkew
) {}
