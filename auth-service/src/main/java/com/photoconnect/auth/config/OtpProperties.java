package com.photoconnect.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * OTP-flow tunables. Bound from {@code app.otp.*}.
 *
 * <p>{@link #provider()} chooses which {@link com.photoconnect.auth.service.OtpDeliveryService}
 * implementation is active — Spring's {@code @ConditionalOnProperty} on each
 * impl ensures exactly one bean is created.</p>
 *
 * <p>{@link #devMode()} is orthogonal: it controls whether {@code /otp/send}
 * echoes the code in the response body (useful for local testing even when
 * the real SMS provider is enabled in a pre-prod env with whitelisted numbers).
 * MUST be false in production.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        @NotNull Provider provider,
        boolean devMode,
        @Min(4) int length,
        @NotNull Duration ttl,
        @NotNull Duration resendCooldown,
        @Min(1) int maxAttempts
) {
    public enum Provider { DEV, MSG91 }
}
