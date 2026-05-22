package com.photoconnect.customer.client;

import java.time.Instant;
import java.util.UUID;

/**
 * Tolerant reader for photographer-service's FeedItemResponse.
 * Only the fields the favorites list needs — Jackson ignores the rest.
 */
public record PortfolioItemSummary(
        UUID id,
        String mediaType,
        String category,
        String mimeType,
        String publicUrl,
        Instant uploadedAt,
        PhotographerSnippet photographer
) {
    public record PhotographerSnippet(UUID id, String displayName, String location) {}
}
