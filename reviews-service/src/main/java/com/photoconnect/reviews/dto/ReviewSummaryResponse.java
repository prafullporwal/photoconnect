package com.photoconnect.reviews.dto;

import java.util.UUID;

/**
 * Aggregate ratings response. Always returns a value (even for photographers
 * with zero reviews) so the front-end never has to special-case a 404.
 *
 * @param photographerProfileId  echo of the path param
 * @param averageRating          mean star rating, 0.0 when {@code reviewCount == 0}
 * @param reviewCount            total number of reviews
 */
public record ReviewSummaryResponse(
        UUID photographerProfileId,
        double averageRating,
        long reviewCount
) {}
