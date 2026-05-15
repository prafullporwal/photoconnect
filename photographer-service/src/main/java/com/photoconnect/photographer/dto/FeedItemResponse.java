package com.photoconnect.photographer.dto;

import com.photoconnect.photographer.domain.MediaType;

import java.time.Instant;
import java.util.UUID;

/**
 * One tile on the marketplace browse feed.
 *
 * <p>This is a <em>denormalised</em> view: each row carries the portfolio
 * item's media info PLUS a slim {@link PhotographerSnippet} so the SPA can
 * render a tile with the photographer's name overlay and link to their
 * profile, all without a follow-up request per tile.</p>
 *
 * <p>If photographer-service later evolves to read-replicas or sharded
 * portfolio storage, this DTO becomes the seam — the controller can hide
 * how the join happens behind the same response shape.</p>
 */
public record FeedItemResponse(
        UUID id,
        MediaType mediaType,
        String category,
        String mimeType,
        String publicUrl,
        Instant uploadedAt,
        PhotographerSnippet photographer
) {
    /** The minimum info the feed tile needs to render the overlay + click-through. */
    public record PhotographerSnippet(UUID id, String displayName, String location) {}
}
