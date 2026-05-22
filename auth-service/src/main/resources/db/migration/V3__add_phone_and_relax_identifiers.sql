-- =============================================================================
-- V3 — phone-based (OTP) signup support
-- =============================================================================
-- Adds an OTP-signup path alongside the existing email/password path.
--
-- Schema rule going forward: a user is identified by email OR phone (or both).
-- That's enforced by a CHECK constraint rather than a discriminator column —
-- keeps the model simple while we have just two identifier types.
--
-- password_hash also becomes nullable: an OTP-only account has no password.
-- The application layer rejects password-login attempts when the hash is null
-- (no plaintext ever compares-equal to a null hash, but failing fast is clearer).
-- =============================================================================

ALTER TABLE users ALTER COLUMN email         DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- E.164 format only (e.g. +919876543210). Validated again at the DTO layer,
-- but the DB is the last line of defence — a future bulk-insert script can't
-- bypass it.
ALTER TABLE users
    ADD CONSTRAINT chk_users_phone_e164
    CHECK (phone IS NULL OR phone ~ '^\+[1-9][0-9]{7,14}$');

-- A user must be reachable by something.
ALTER TABLE users
    ADD CONSTRAINT chk_users_has_identifier
    CHECK (email IS NOT NULL OR phone IS NOT NULL);

-- Unique amongst active users; soft-deleted rows free the number.
CREATE UNIQUE INDEX uk_users_phone_active
    ON users (phone)
    WHERE deleted_at IS NULL AND phone IS NOT NULL;

COMMENT ON COLUMN users.phone IS 'E.164 phone, e.g. +919876543210; unique among active users';
