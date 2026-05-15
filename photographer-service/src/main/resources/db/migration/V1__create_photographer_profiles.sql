-- =============================================================================
-- V1: Photographer profile schema
-- =============================================================================
-- photographer_db is created by docker-compose's init script.
-- Flyway runs this migration on first startup and records it in
-- flyway_schema_history so it never runs again.
--
-- TWO tables:
--   photographer_profiles  — one row per photographer
--   photographer_specialties — child table for the @ElementCollection;
--     no PK of its own, joined to profiles by photographer_id
-- =============================================================================

CREATE TABLE photographer_profiles (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id              UUID          NOT NULL,
    display_name         VARCHAR(100)  NOT NULL,
    bio                  TEXT,
    location             VARCHAR(200)  NOT NULL,
    years_of_experience  INTEGER       NOT NULL DEFAULT 0,
    price_per_hour       NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    available            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    version              BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_photographer_profiles
        PRIMARY KEY (id),

    -- One photographer profile per user_id
    CONSTRAINT uq_photographer_profiles_user_id
        UNIQUE (user_id),

    CONSTRAINT chk_years_of_experience
        CHECK (years_of_experience >= 0),

    CONSTRAINT chk_price_per_hour
        CHECK (price_per_hour >= 0)
);

-- @ElementCollection for List<String> specialties
-- No surrogate PK: specialties only exist as part of a profile.
CREATE TABLE photographer_specialties (
    photographer_id  UUID          NOT NULL,
    specialty        VARCHAR(100)  NOT NULL,

    CONSTRAINT fk_specialties_profile
        FOREIGN KEY (photographer_id)
        REFERENCES photographer_profiles (id)
        ON DELETE CASCADE   -- removing a profile removes its specialties
);

-- Indexes
CREATE INDEX idx_photographer_profiles_available
    ON photographer_profiles (available);   -- drives the browse query

CREATE INDEX idx_photographer_specialties_profile
    ON photographer_specialties (photographer_id);  -- JOIN optimisation
