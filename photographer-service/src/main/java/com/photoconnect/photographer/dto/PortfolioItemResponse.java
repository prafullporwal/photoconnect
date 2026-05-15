package com.photoconnect.photographer.dto;

import com.photoconnect.photographer.domain.MediaType;

import java.time.Instant;
import java.util.UUID;

/** What the SPA receives for every portfolio item. */
public record PortfolioItemResponse(
        UUID id,
        UUID photographerProfileId,
        MediaType mediaType,
        String category,
        String mimeType,
        long sizeBytes,
        /** Direct-fetch URL (anonymous read on the bucket) — browser uses this in <img> / <video>. */
        String publicUrl,
        int displayOrder,
        Instant uploadedAt
) {}
