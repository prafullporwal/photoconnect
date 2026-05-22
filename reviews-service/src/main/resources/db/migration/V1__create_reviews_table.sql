-- =============================================================================
-- V1: Reviews table (PostgreSQL syntax)
-- =============================================================================
-- A review is written by a customer against a (photographer-profile,
-- photographer-user) pair. We store BOTH photographer ids for the same reason
-- the inquiries table does:
--   * photographer_profile_id      → deep-linking back to the profile page,
--                                    aggregate-by-profile queries.
--   * photographer_user_id         → the underlying auth identity, useful if a
--                                    photographer ever rebuilds their profile
--                                    (new profile id, same user).
--
-- Integrity rules pushed to the DB:
--   * rating must be 1..5
--   * at most one review per (customer, photographer-profile)
--
-- The "must have a completed booking" check lives in the service layer —
-- it can only be answered by calling customer-service and we don't want a
-- foreign-key chain across services.
-- =============================================================================

CREATE TABLE reviews (
    id                        UUID         NOT NULL DEFAULT gen_random_uuid(),
    customer_id               UUID         NOT NULL,
    photographer_profile_id   UUID         NOT NULL,
    photographer_user_id      UUID         NOT NULL,
    inquiry_id                UUID         NOT NULL,
    rating                    SMALLINT     NOT NULL,
    body                      TEXT,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                   BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_reviews PRIMARY KEY (id),

    -- 1..5 inclusive — same range exposed by the DTO @Min/@Max. Belt + braces.
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),

    -- One review per (customer, photographer-profile). Even if a customer
    -- has multiple completed engagements with the same photographer, the
    -- product rule is one review. The DB enforces it; the service catches
    -- the unique-violation and translates to a 409.
    CONSTRAINT uq_reviews_customer_photographer
        UNIQUE (customer_id, photographer_profile_id)
);

-- Hot path: "reviews for this photographer, newest first" + "what's the avg rating"
CREATE INDEX idx_reviews_photographer_profile_created
    ON reviews (photographer_profile_id, created_at DESC);

-- "my reviews" — customer profile screen
CREATE INDEX idx_reviews_customer_created
    ON reviews (customer_id, created_at DESC);
