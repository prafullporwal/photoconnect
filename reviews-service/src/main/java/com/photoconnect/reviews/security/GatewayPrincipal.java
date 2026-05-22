package com.photoconnect.reviews.security;

import com.photoconnect.reviews.domain.Role;

import java.util.UUID;

/** Identity forwarded by api-gateway as {@code X-User-Id} / {@code X-User-Role}. */
public record GatewayPrincipal(UUID userId, Role role) {}
