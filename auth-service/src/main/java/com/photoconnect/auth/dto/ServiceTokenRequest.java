package com.photoconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound request to mint a service-to-service token. Mirrors the shape of
 * an OAuth2 client-credentials grant ({@code grant_type=client_credentials}),
 * trimmed to just what we need.
 *
 * @param clientId      identifier of the calling service (e.g. {@code "customer-service"})
 * @param clientSecret  shared secret presented in cleartext over the wire;
 *                      auth-service compares it against a BCrypt hash held in config
 */
public record ServiceTokenRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret
) {}
