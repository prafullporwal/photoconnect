-- =============================================================================
-- V1: Customer profiles + Inquiries (MySQL 8 syntax)
-- =============================================================================
-- customer_db is created by docker-compose's MySQL init script.
-- MySQL differences vs. PostgreSQL:
--   * UUID stored as VARCHAR(36)  (no native UUID type)
--   * DATETIME(6)                 (no TIMESTAMPTZ; we always write UTC)
--   * gen_random_uuid() unavailable — Hibernate generates UUIDs in Java
--   * No partial UNIQUE indexes — full UNIQUE is fine here
-- =============================================================================

CREATE TABLE customers (
    id                        VARCHAR(36)   NOT NULL,
    user_id                   VARCHAR(36)   NOT NULL,
    display_name              VARCHAR(100)  NOT NULL,
    location                  VARCHAR(200),
    phone_number              VARCHAR(30),
    preferred_contact_method  VARCHAR(20)   NOT NULL DEFAULT 'EMAIL',
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    version                   BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_user_id UNIQUE (user_id),
    CONSTRAINT chk_contact_method CHECK (preferred_contact_method IN ('EMAIL', 'PHONE', 'PLATFORM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE inquiries (
    id                        VARCHAR(36)   NOT NULL,
    customer_id               VARCHAR(36)   NOT NULL,
    photographer_profile_id   VARCHAR(36)   NOT NULL,
    photographer_user_id      VARCHAR(36)   NOT NULL,
    event_date                DATE          NOT NULL,
    event_type                VARCHAR(50)   NOT NULL,
    location                  VARCHAR(200),
    budget                    DECIMAL(10,2),
    message                   TEXT          NOT NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    version                   BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_inquiries PRIMARY KEY (id),
    CONSTRAINT chk_inquiry_status CHECK (status IN ('NEW', 'READ', 'RESPONDED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes that drive the inbox queries
CREATE INDEX idx_inquiries_customer
    ON inquiries (customer_id, created_at DESC);     -- "my outbox" query

CREATE INDEX idx_inquiries_photographer_user
    ON inquiries (photographer_user_id, created_at DESC);  -- "my inbox" query

CREATE INDEX idx_inquiries_status
    ON inquiries (status);
