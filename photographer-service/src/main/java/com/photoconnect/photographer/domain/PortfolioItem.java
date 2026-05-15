package com.photoconnect.photographer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a single uploaded sample asset.
 *
 * <h2>Where the bytes live</h2>
 * <p>This row only holds metadata. The actual file lives in object storage
 * (MinIO locally, S3 in Phase 2) under {@code storageKey}. {@code publicUrl}
 * is the resolved direct-fetch URL the SPA renders — bucket policy makes the
 * bucket anonymously readable, so the browser can {@code <img>}/{@code <video>}
 * the URL without going through this service.</p>
 *
 * <h2>Why both {@code mediaType} and {@code mimeType}?</h2>
 * <p>{@code mediaType} is editorial (IMAGE/VIDEO/REEL) — drives layout choices
 * in the SPA. {@code mimeType} is the actual transport encoding
 * (image/jpeg, video/mp4) — used to set the {@code Content-Type} on the
 * object and to drive {@code <video>} fallbacks.</p>
 */
@Entity
@Table(name = "portfolio_items")
@Getter
@Setter
@NoArgsConstructor
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "photographer_profile_id", nullable = false)
    private UUID photographerProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    /**
     * Editorial bucket — free-form so photographers can use any vocabulary
     * (often matches one of their declared specialties: "wedding", "portrait").
     */
    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Object key inside the bucket — e.g. {@code photographers/{profileId}/{itemId}.jpg}. */
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    /** Resolved direct-fetch URL for the SPA. Recomputed from the storage key. */
    @Column(name = "public_url", nullable = false, length = 1000)
    private String publicUrl;

    /** Manual ordering within a category — lower numbers shown first. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void onPrePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }
}
