package com.photoconnect.photographer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API response for a photographer profile.
 *
 * <p>Returned by all read and write endpoints. The front-end uses
 * {@code id} (profile UUID) for subsequent reads, and {@code userId} to
 * correlate with the authenticated user from auth-service.</p>
 */
public record PhotographerProfileResponse(
        UUID id,
        UUID userId,
        String displayName,
        String bio,
        String location,
        int yearsOfExperience,
        BigDecimal pricePerHour,
        boolean available,
        List<String> specialties,
        Instant createdAt,
        Instant updatedAt
) {}
