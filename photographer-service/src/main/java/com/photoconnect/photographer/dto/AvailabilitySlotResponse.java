package com.photoconnect.photographer.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response shape for a single availability slot.
 *
 * <p>{@code photographerProfileId} is included so the SPA can keep state keyed
 * by it without an extra round-trip. {@code note} is nullable — most slots
 * won't carry one.</p>
 */
public record AvailabilitySlotResponse(
        UUID id,
        UUID photographerProfileId,
        LocalDate availableDate,
        String note,
        Instant createdAt
) {}
