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
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link FavoriteService} — all dependencies mocked.
 *
 * <p>We use the BDDMockito style ({@code given/then}) to keep each test
 * structured as "given some precondition, when something happens, then
 * assert something" — the same mental model as the user flow.</p>
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteRepository repository;
    @Mock private PhotographerClient photographerClient;

    @InjectMocks private FavoriteService service;

    private UUID customerId;
    private UUID portfolioItemId;
    private PortfolioItemSummary summary;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        portfolioItemId = UUID.randomUUID();
        summary = new PortfolioItemSummary(
                portfolioItemId,
                "PHOTO",
                "wedding",
                "image/jpeg",
                "https://cdn.example.com/img.jpg",
                Instant.now(),
                new PortfolioItemSummary.PhotographerSnippet(UUID.randomUUID(), "Alice Camera", "London"));
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_newFavorite_persistsAndReturns() {
        given(repository.findByCustomerIdAndPortfolioItemId(customerId, portfolioItemId))
                .willReturn(Optional.empty());
        given(photographerClient.getPortfolioItem(portfolioItemId))
                .willReturn(summary);

        Favorite saved = stubSave();
        given(repository.save(any(Favorite.class))).willReturn(saved);

        FavoriteResponse response = service.save(customerId, portfolioItemId);

        assertThat(response.portfolioItemId()).isEqualTo(portfolioItemId);
        assertThat(response.item()).isNull();   // withoutItem factory used
        then(repository).should().save(any(Favorite.class));
    }

    @Test
    void save_alreadySaved_returnsExistingWithoutFeign() {
        Favorite existing = stubSave();
        given(repository.findByCustomerIdAndPortfolioItemId(customerId, portfolioItemId))
                .willReturn(Optional.of(existing));

        FavoriteResponse response = service.save(customerId, portfolioItemId);

        assertThat(response.portfolioItemId()).isEqualTo(portfolioItemId);
        // Feign must NOT be called — the fast path short-circuits before the verify call
        then(photographerClient).should(never()).getPortfolioItem(any());
        then(repository).should(never()).save(any());
    }

    @Test
    void save_portfolioItemNotFound_throws404() {
        given(repository.findByCustomerIdAndPortfolioItemId(customerId, portfolioItemId))
                .willReturn(Optional.empty());
        given(photographerClient.getPortfolioItem(portfolioItemId))
                .willThrow(feignNotFound());

        assertThatThrownBy(() -> service.save(customerId, portfolioItemId))
                .isInstanceOf(PhotographerNotFoundException.class);
        then(repository).should(never()).save(any());
    }

    @Test
    void save_photographerServiceDown_throws503() {
        given(repository.findByCustomerIdAndPortfolioItemId(customerId, portfolioItemId))
                .willReturn(Optional.empty());
        given(photographerClient.getPortfolioItem(portfolioItemId))
                .willThrow(feignServerError());

        assertThatThrownBy(() -> service.save(customerId, portfolioItemId))
                .isInstanceOf(PhotographerServiceUnavailableException.class);
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    void remove_existingFavorite_deletesRow() {
        given(repository.deleteByCustomerAndPortfolioItem(customerId, portfolioItemId))
                .willReturn(1);

        service.remove(customerId, portfolioItemId);

        then(repository).should().deleteByCustomerAndPortfolioItem(customerId, portfolioItemId);
    }

    @Test
    void remove_notPreviouslySaved_silentNoOp() {
        given(repository.deleteByCustomerAndPortfolioItem(customerId, portfolioItemId))
                .willReturn(0);   // nothing matched

        service.remove(customerId, portfolioItemId);   // must not throw
    }

    // ── status ────────────────────────────────────────────────────────────────

    @Test
    void status_saved_returnsTrue() {
        given(repository.existsByCustomerIdAndPortfolioItemId(customerId, portfolioItemId))
                .willReturn(true);

        FavoriteStatusResponse result = service.getStatus(customerId, portfolioItemId);

        assertThat(result.favorited()).isTrue();
    }

    @Test
    void status_notSaved_returnsFalse() {
        given(repository.existsByCustomerIdAndPortfolioItemId(customerId, portfolioItemId))
                .willReturn(false);

        FavoriteStatusResponse result = service.getStatus(customerId, portfolioItemId);

        assertThat(result.favorited()).isFalse();
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_enrichesWithPortfolioItem() {
        Favorite fav = stubSave();
        given(repository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .willReturn(List.of(fav));
        given(photographerClient.getPortfolioItem(portfolioItemId))
                .willReturn(summary);

        List<FavoriteResponse> results = service.list(customerId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).item()).isNotNull();
        assertThat(results.get(0).item().photographer().displayName()).isEqualTo("Alice Camera");
    }

    @Test
    void list_deletedPortfolioItem_returnsNullItem() {
        Favorite fav = stubSave();
        given(repository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .willReturn(List.of(fav));
        given(photographerClient.getPortfolioItem(portfolioItemId))
                .willThrow(feignNotFound());   // item was deleted

        List<FavoriteResponse> results = service.list(customerId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).item()).isNull();   // tombstone
    }

    @Test
    void list_photographerServiceDegraded_stillReturnsList() {
        Favorite fav = stubSave();
        given(repository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .willReturn(List.of(fav));
        given(photographerClient.getPortfolioItem(portfolioItemId))
                .willThrow(feignServerError());   // service is down

        // Must NOT throw — the list degrades gracefully with null item
        List<FavoriteResponse> results = service.list(customerId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).item()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Favorite stubSave() {
        Favorite f = new Favorite();
        f.setCustomerId(customerId);
        f.setPortfolioItemId(portfolioItemId);
        // Manually stamp createdAt because @PrePersist doesn't fire outside a real EntityManager
        try {
            var field = Favorite.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(f, Instant.now());
            var idField = Favorite.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(f, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return f;
    }

    private static FeignException.NotFound feignNotFound() {
        return new FeignException.NotFound(
                "Not Found",
                Request.create(Request.HttpMethod.GET, "/", Map.of(), null, null, null),
                null, Map.of());
    }

    private static FeignException feignServerError() {
        return new FeignException.InternalServerError(
                "Internal Server Error",
                Request.create(Request.HttpMethod.GET, "/", Map.of(), null, null, null),
                null, Map.of());
    }
}
