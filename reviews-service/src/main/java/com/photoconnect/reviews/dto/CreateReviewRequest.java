package com.photoconnect.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Inbound payload for {@code POST /api/v1/reviews}.
 *
 * <p>Validation here covers the obvious edges; the cross-service "completed
 * booking" rule cannot be expressed at this layer and lives in
 * {@code ReviewService}.</p>
 *
 * @param photographerProfileId  who is being reviewed (validated via Feign)
 * @param rating                 integer 1..5 inclusive
 * @param body                   optional free text, up to 2000 chars; null/blank
 *                               is acceptable so customers can leave a rating only
 */
public record CreateReviewRequest(
        @NotNull UUID photographerProfileId,

        @NotNull
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must be at most 5")
        Integer rating,

        @Size(max = 2000, message = "body must not exceed 2000 characters")
        String body
) {}
