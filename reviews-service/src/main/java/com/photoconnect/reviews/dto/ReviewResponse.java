package com.photoconnect.reviews.dto;

import java.time.Instant;
import java.util.UUID;

/** Response shape for a single review. */
public record ReviewResponse(
        UUID id,
        UUID customerId,
        UUID photographerProfileId,
        UUID photographerUserId,
        UUID inquiryId,
        int rating,
        String body,
        Instant createdAt,
        Instant updatedAt
) {}
