-- =============================================================================
-- V2 — refresh_tokens table
-- =============================================================================
-- One row per issued refresh token. We store the jti (token_id) so we can
-- revoke a single token or all tokens for a user, and detect re-use of an
-- already-rotated token.
-- =============================================================================

CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users(id),
    token_id     VARCHAR(64)  NOT NULL UNIQUE,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

COMMENT ON TABLE  refresh_tokens IS 'JWT refresh token records; jti stored as token_id';
COMMENT ON COLUMN refresh_tokens.token_id IS 'JWT jti claim (random UUID per issue)';
