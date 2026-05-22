package com.photoconnect.auth.dto;

import java.time.Instant;

/**
 * Response for {@code POST /api/v1/auth/otp/send}.
 *
 * <p>{@code devCode} is populated ONLY when {@code app.otp.dev-mode=true};
 * never present in pre-prod/prod responses.</p>
 */
public record SendOtpResponse(
        String phone,
        Instant expiresAt,
        String devCode
) {}
