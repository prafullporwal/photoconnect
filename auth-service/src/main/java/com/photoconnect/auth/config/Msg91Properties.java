package com.photoconnect.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MSG91 Flow API credentials and template. Bound from {@code app.otp.msg91.*}.
 *
 * <p>Only loaded when {@code app.otp.provider=msg91} (see {@link OtpDeliveryConfig}).
 * Validation runs at startup — missing auth-key or template-id will fail the
 * app fast rather than at first SMS send.</p>
 *
 * @param baseUrl    MSG91 host, normally {@code https://control.msg91.com}.
 * @param authKey    Per-account API key from the MSG91 dashboard.
 * @param templateId DLT-approved template ID; the template body must reference
 *                   a single variable (we pass the OTP as {@code var}).
 * @param senderId   6-character DLT-registered header. Optional in dev/sandbox;
 *                   mandatory once DLT goes live in prod.
 */
@Validated
@ConfigurationProperties(prefix = "app.otp.msg91")
public record Msg91Properties(
        @NotBlank String baseUrl,
        @NotBlank String authKey,
        @NotBlank String templateId,
        String senderId
) {}
