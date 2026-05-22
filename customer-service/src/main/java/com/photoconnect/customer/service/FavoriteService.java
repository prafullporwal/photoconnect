package com.photoconnect.customer.service;

import com.photoconnect.customer.client.PhotographerClient;
import com.photoconnect.customer.client.PortfolioItemSummary;
import com.photoconnect.customer.domain.Favorite;
import com.photoconnect.customer.dto.FavoriteResponse;
import com.photoconnect.customer.dto.FavoriteStatusResponse;
import com.photoconnect.customer.exception.PhotographerNotFoundException;
import com.photoconnect.customer.exception.PhotographerServiceUnavailableException;
import com.photoconnect.customer.repository.FavoriteRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for customer favorites ("save content" bookmarks).
 *
 * <p>Customers save individual portfolio items (photos / videos / reels),
 * not photographer profiles. This lets the saved page look and feel exactly
 * like the browse feed — only showing content the customer explicitly liked.</p>
 *
 * <h2>Idempotency</h2>
 * <p>Both write operations are idempotent — re-saving or un-saving are no-op
 * successes. The UI heart toggle can fire twice on a flaky network without
 * showing an error.</p>
 *
 * <h2>Enrichment</h2>
 * <p>The list endpoint calls photographer-service once per row to hydrate the
 * response with full media + photographer info. If a row's item was deleted,
 * {@code item} is null and the SPA renders a tombstone.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository repository;
    private final PhotographerClient photographerClient;

    // ── Writes ────────────────────────────────────────────────────────────────

    public FavoriteResponse save(UUID customerId, UUID portfolioItemId) {

        // Fast path — already saved; skip the cross-service call.
        var existing = repository.findByCustomerIdAndPortfolioItemId(customerId, portfolioItemId);
        if (existing.isPresent()) {
            Favorite f = existing.get();
            return FavoriteResponse.withoutItem(f.getId(), f.getPortfolioItemId(), f.getCreatedAt());
        }

        // Verify the item exists before persisting the row.
        verifyPortfolioItemExists(portfolioItemId);

        Favorite favorite = new Favorite();
        favorite.setCustomerId(customerId);
        favorite.setPortfolioItemId(portfolioItemId);

        try {
            Favorite saved = repository.save(favorite);
            log.info("Customer {} saved portfolio item {}", customerId, portfolioItemId);
            return FavoriteResponse.withoutItem(saved.getId(), saved.getPortfolioItemId(), saved.getCreatedAt());

        } catch (DataIntegrityViolationException e) {
            // Concurrent save hit the unique constraint — return the winner's row.
            Favorite winner = repository
                    .findByCustomerIdAndPortfolioItemId(customerId, portfolioItemId)
                    .orElseThrow(() -> e);
            return FavoriteResponse.withoutItem(winner.getId(), winner.getPortfolioItemId(), winner.getCreatedAt());
        }
    }

    public void remove(UUID customerId, UUID portfolioItemId) {
        int removed = repository.deleteByCustomerAndPortfolioItem(customerId, portfolioItemId);
        if (removed > 0) {
            log.info("Customer {} unsaved portfolio item {}", customerId, portfolioItemId);
        }
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::enrich)
                .toList();
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse getStatus(UUID customerId, UUID portfolioItemId) {
        return new FavoriteStatusResponse(
                repository.existsByCustomerIdAndPortfolioItemId(customerId, portfolioItemId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FavoriteResponse enrich(Favorite f) {
        PortfolioItemSummary summary = null;
        try {
            summary = photographerClient.getPortfolioItem(f.getPortfolioItemId());
        } catch (FeignException.NotFound e) {
            log.debug("Favorite {} points at deleted portfolio item {}", f.getId(), f.getPortfolioItemId());
        } catch (FeignException e) {
            log.warn("Feign enrichment failed for favorite {}: status={}", f.getId(), e.status());
        }
        return new FavoriteResponse(f.getId(), f.getPortfolioItemId(), summary, f.getCreatedAt());
    }

    private void verifyPortfolioItemExists(UUID portfolioItemId) {
        try {
            photographerClient.getPortfolioItem(portfolioItemId);
        } catch (FeignException.NotFound e) {
            throw new PhotographerNotFoundException(portfolioItemId);
        } catch (FeignException e) {
            log.warn("Feign verify failed for portfolio item {}: status={}", portfolioItemId, e.status());
            throw new PhotographerServiceUnavailableException(e);
        }
    }
}
