-- =============================================================================
-- V3: Add COMPLETED to the inquiry status CHECK constraint (MySQL 8 syntax)
-- =============================================================================
-- Reviews-service requires a "completed booking" precondition. Until a proper
-- booking-service ships in Phase 2, we proxy that concept onto inquiries via
-- a new terminal status: COMPLETED.
--
-- MySQL doesn't support `ALTER CHECK ... ADD VALUE`; we drop the old constraint
-- and add a new one with the expanded allow-list.
-- =============================================================================

ALTER TABLE inquiries
    DROP CONSTRAINT chk_inquiry_status;

ALTER TABLE inquiries
    ADD CONSTRAINT chk_inquiry_status
        CHECK (status IN ('NEW', 'READ', 'RESPONDED', 'CLOSED', 'COMPLETED'));

-- Drives the reviews-service Feign predicate: does a COMPLETED engagement exist?
-- Composite on (customer_id, photographer_profile_id, status) so the index can
-- satisfy the lookup without touching the row.
CREATE INDEX idx_inquiries_engagement
    ON inquiries (customer_id, photographer_profile_id, status);
