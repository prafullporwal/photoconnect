package com.photoconnect.photographer.security;

import com.photoconnect.photographer.domain.Role;

import java.util.UUID;

/**
 * The authenticated caller's identity as forwarded by api-gateway.
 *
 * <p>api-gateway validates the JWT, extracts the {@code sub} (userId UUID)
 * and {@code role} claims, and stamps them as {@code X-User-Id} /
 * {@code X-User-Role} on every request it forwards. {@link GatewayAuthenticationFilter}
 * reads those headers and wraps the values in this record, which becomes the
 * {@code principal} inside Spring Security's {@code Authentication} object.</p>
 *
 * <p>In controller methods, inject it with:</p>
 * <pre>{@code
 *   @AuthenticationPrincipal GatewayPrincipal caller
 * }</pre>
 */
public record GatewayPrincipal(UUID userId, Role role) {}
