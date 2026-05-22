package com.photoconnect.customer.dto;

import com.photoconnect.customer.client.PortfolioItemSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * Response shape for a single saved content item.
 *
 * <p>The list endpoint enriches each row via Feign so the SPA can render
 * content tiles without a second round-trip. If the portfolio item has been
 * deleted since the customer saved it, {@code item} is {@code null} and
 * the front-end shows a tombstone tile with an "Unsave" button.</p>
 */
public record FavoriteResponse(
        UUID id,
        UUID portfolioItemId,
        PortfolioItemSummary item,    // nullable when the content was deleted
        Instant createdAt
) {
    /** Convenience factory for the toggle endpoints that don't need enrichment. */
    public static FavoriteResponse withoutItem(UUID id, UUID portfolioItemId, Instant createdAt) {
        return new FavoriteResponse(id, portfolioItemId, null, createdAt);
    }
}
