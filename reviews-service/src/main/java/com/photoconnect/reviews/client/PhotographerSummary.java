package com.photoconnect.reviews.client;

import java.util.UUID;

/**
 * Slim view of a photographer profile — only the fields we need.
 *
 * <p>Jackson will fill these fields from photographer-service's full response
 * and ignore everything else. This is a deliberate <em>tolerant reader</em>:
 * photographer-service can add fields to its response without breaking us.</p>
 */
public record PhotographerSummary(
        UUID id,
        UUID userId,
        String displayName
) {}
