-- =============================================================================
-- V1 — auth_db schema bootstrap: users table
-- =============================================================================
-- Owner: auth-service. NO other service should issue DDL or DML against this
-- schema. Cross-service reads happen over HTTP/events, never via DB joins.
-- =============================================================================

CREATE TABLE users (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('PHOTOGRAPHER', 'CUSTOMER')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    -- audit columns populated by Spring Data JPA Auditing
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),

    -- optimistic locking (matches @Version on the entity)
    version         BIGINT       NOT NULL DEFAULT 0
);

-- Email must be unique amongst ACTIVE (non-soft-deleted) users.
-- Partial index = a soft-deleted user freeing up their email is fine.
CREATE UNIQUE INDEX uk_users_email_active
    ON users (LOWER(email))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_deleted_at ON users (deleted_at);

COMMENT ON TABLE  users IS 'PhotoConnect platform users (photographers and customers)';
COMMENT ON COLUMN users.role IS 'PHOTOGRAPHER or CUSTOMER';
COMMENT ON COLUMN users.deleted_at IS 'Soft-delete marker; NULL means active';
COMMENT ON COLUMN users.version IS 'Optimistic locking counter';
