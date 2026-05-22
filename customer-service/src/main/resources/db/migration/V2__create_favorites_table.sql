-- =============================================================================
-- V2: Customer favorites ("save photographer" bookmarks) — MySQL 8 syntax
-- =============================================================================
-- A favorite is a (customer, photographer) pair the customer has bookmarked.
-- The composite UNIQUE constraint is the integrity layer — service code
-- translates a duplicate-key violation into "already saved" if a race wins.
--
-- We deliberately do NOT denormalise photographer_user_id here (unlike
-- inquiries). Favorites is purely customer-side reading in MVP; if Phase 2
-- adds a photographer-side "who saved me" view, we add it then via V3.
-- =============================================================================

CREATE TABLE favorites (
    id                        VARCHAR(36)   NOT NULL,
    customer_id               VARCHAR(36)   NOT NULL,
    photographer_profile_id   VARCHAR(36)   NOT NULL,
    created_at                DATETIME(6)   NOT NULL,

    CONSTRAINT pk_favorites PRIMARY KEY (id),

    -- One favorite per (customer, photographer) pair. The unique key also
    -- becomes the lookup index for "is this saved?" status checks.
    CONSTRAINT uq_favorites_customer_photographer
        UNIQUE (customer_id, photographer_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Drives the "my favorites, newest first" listing.
CREATE INDEX idx_favorites_customer_created
    ON favorites (customer_id, created_at DESC);
