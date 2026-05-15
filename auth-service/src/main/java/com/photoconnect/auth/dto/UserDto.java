package com.photoconnect.auth.dto;

import com.photoconnect.auth.domain.Role;

import java.time.Instant;
import java.util.UUID;

/** Returned by {@code GET /api/v1/auth/me}. No password, no internals. */
public record UserDto(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {}
