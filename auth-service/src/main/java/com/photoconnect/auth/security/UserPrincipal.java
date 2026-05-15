package com.photoconnect.auth.security;

import com.photoconnect.auth.domain.Role;

import java.util.UUID;

/**
 * Authenticated-user payload extracted from a JWT, attached to the
 * SecurityContext, and reachable from controllers via
 * {@code @AuthenticationPrincipal UserPrincipal principal}.
 *
 * <p>{@code jti} is included so {@code /logout} knows which access-token
 * id to blacklist.</p>
 */
public record UserPrincipal(UUID userId, String email, Role role, String jti) {}
