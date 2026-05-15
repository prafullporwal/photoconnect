-- =============================================================================
-- V2: Portfolio items — sample media (images / video / reels) per photographer
-- =============================================================================
-- One row per uploaded asset. The binary lives in object storage (MinIO/S3);
-- this table stores only the metadata + a stable storage key and a public URL
-- that the SPA renders directly.
-- =============================================================================

CREATE TABLE portfolio_items (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
    photographer_profile_id  UUID         NOT NULL,
    media_type               VARCHAR(20)  NOT NULL,
    category                 VARCHAR(100) NOT NULL,
    mime_type                VARCHAR(100) NOT NULL,
    size_bytes               BIGINT       NOT NULL,
    storage_key              VARCHAR(500) NOT NULL,
    public_url               VARCHAR(1000) NOT NULL,
    display_order            INTEGER      NOT NULL DEFAULT 0,
    uploaded_at              TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_portfolio_items
        PRIMARY KEY (id),

    -- Cascade delete: if a photographer profile is removed, drop their media too.
    -- The actual S3/MinIO objects are best-effort cleaned by the service layer
    -- on profile delete; the DB row cleanup happens automatically here.
    CONSTRAINT fk_portfolio_profile
        FOREIGN KEY (photographer_profile_id)
        REFERENCES photographer_profiles (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_portfolio_media_type
        CHECK (media_type IN ('IMAGE', 'VIDEO', 'REEL')),

    CONSTRAINT chk_portfolio_size
        CHECK (size_bytes > 0),

    -- Same object key can't be inserted twice (defensive — service generates UUIDs)
    CONSTRAINT uq_portfolio_storage_key
        UNIQUE (storage_key)
);

-- Composite index drives the most common query:
-- "show me this photographer's content, optionally filtered by type and category"
CREATE INDEX idx_portfolio_profile_lookup
    ON portfolio_items (photographer_profile_id, media_type, category, display_order);
