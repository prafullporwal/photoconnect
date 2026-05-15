package com.photoconnect.photographer.repository;

import com.photoconnect.photographer.domain.MediaType;

import java.time.Instant;
import java.util.UUID;

/**
 * Flat JPQL projection for the marketplace feed query. The repository can
 * return this directly via JPA's {@code new com.example.FlatRow(...)}
 * constructor expression — assembling the nested {@code FeedItemResponse}
 * happens in the service layer.
 *
 * <p>Records work as JPQL constructor targets in JPA 3.2+, so no class-level
 * adjustments are needed.</p>
 */
public record FeedRow(
        UUID itemId,
        MediaType mediaType,
        String category,
        String mimeType,
        String publicUrl,
        Instant uploadedAt,
        UUID photographerProfileId,
        String photographerName,
        String photographerLocation
) {}
