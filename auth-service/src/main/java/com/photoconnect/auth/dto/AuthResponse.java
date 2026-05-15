package com.photoconnect.auth.dto;

import com.photoconnect.auth.domain.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload for register / login / refresh.
 *
 * <p>Returns BOTH tokens plus the access token's expiry so clients can
 * proactively refresh shortly before it expires (recommended pattern).</p>
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        UUID userId,
        String email,
        Role role
) {}
