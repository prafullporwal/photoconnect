-- =============================================================================
-- V3: Photographer availability calendar
-- =============================================================================
-- One row per (photographer, date) the photographer has marked as available.
-- The UNIQUE constraint guarantees a date can't appear twice for the same
-- photographer — service code translates the resulting DataIntegrityViolation
-- into a "skip and continue" so bulk inserts stay idempotent.
--
-- Day-level granularity by choice: photography sessions are negotiated via the
-- inquiry thread anyway. If hour-level slots become needed, add a TIME column
-- and widen the UNIQUE constraint in a V? migration.
-- =============================================================================

CREATE TABLE availability_slots (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
    photographer_profile_id  UUID         NOT NULL,
    available_date           DATE         NOT NULL,
    note                     VARCHAR(200),
    created_at               TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_availability_slots
        PRIMARY KEY (id),

    -- Cascade delete keeps the calendar clean if a photographer is removed.
    CONSTRAINT fk_availability_profile
        FOREIGN KEY (photographer_profile_id)
        REFERENCES photographer_profiles (id)
        ON DELETE CASCADE,

    -- One slot per (photographer, date). This is the integrity layer that
    -- makes bulk-add idempotent on retries.
    CONSTRAINT uq_availability_profile_date
        UNIQUE (photographer_profile_id, available_date)
);

-- Drives both the "list my slots" and "what's available for this photographer"
-- queries. ASC because we always render the calendar earliest-first.
CREATE INDEX idx_availability_profile_date
    ON availability_slots (photographer_profile_id, available_date ASC);
