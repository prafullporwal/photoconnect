-- =============================================================================
-- V4: Change favorites to bookmark portfolio items (content), not photographers
-- =============================================================================
-- Previously a favorite linked a customer to a PhotographerProfile.
-- Now it links a customer to a specific PortfolioItem (photo / video / reel).
-- This lets the customer save individual pieces of content they like.
-- =============================================================================

-- Dev: clear stale rows before restructuring (no production data at this stage).
TRUNCATE TABLE favorites;

ALTER TABLE favorites
    DROP INDEX  uq_favorites_customer_photographer,
    DROP COLUMN photographer_profile_id,
    ADD  COLUMN portfolio_item_id VARCHAR(36) NOT NULL AFTER customer_id,
    ADD  CONSTRAINT uq_favorites_customer_item
         UNIQUE (customer_id, portfolio_item_id);

-- The existing idx_favorites_customer_created index (customer_id, created_at)
-- is still valid and useful — no need to recreate it.
