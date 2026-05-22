package com.photoconnect.auth.dto;

import java.time.Instant;

/**
 * Response from {@code POST /api/v1/auth/service-token}.
 *
 * <p>Callers schedule a refresh shortly before {@code expiresAt} — see
 * {@code customer-service}'s {@code ServiceTokenClient} for the cache pattern.</p>
 *
 * @param accessToken  the minted service JWT
 * @param expiresAt    absolute expiry instant (mirrors {@code AuthResponse})
 * @param scope        echo of the granted scope claim, for logging on the caller side
 */
public record ServiceTokenResponse(
        String accessToken,
        Instant expiresAt,
        String scope
) {}
