package com.photoconnect.reviews.service;

import com.photoconnect.reviews.client.CompletedInquiry;
import com.photoconnect.reviews.client.InquiryClient;
import com.photoconnect.reviews.client.PhotographerClient;
import com.photoconnect.reviews.client.PhotographerSummary;
import com.photoconnect.reviews.domain.Review;
import com.photoconnect.reviews.dto.CreateReviewRequest;
import com.photoconnect.reviews.dto.ReviewResponse;
import com.photoconnect.reviews.dto.ReviewSummaryResponse;
import com.photoconnect.reviews.exception.DownstreamServiceUnavailableException;
import com.photoconnect.reviews.exception.DuplicateReviewException;
import com.photoconnect.reviews.exception.NoCompletedBookingException;
import com.photoconnect.reviews.exception.PhotographerNotFoundException;
import com.photoconnect.reviews.mapper.ReviewMapper;
import com.photoconnect.reviews.repository.ReviewRepository;
import com.photoconnect.reviews.repository.ReviewSummary;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link ReviewService} — all dependencies mocked.
 *
 * <p>BDDMockito {@code given/then} style mirrors the user-facing flow:
 * "given a duplicate, when create runs, then it throws 409". Same style the
 * sister services use.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository repository;
    @Mock private ReviewMapper mapper;
    @Mock private InquiryClient inquiryClient;
    @Mock private PhotographerClient photographerClient;

    @InjectMocks private ReviewService service;

    private UUID customerId;
    private UUID photographerProfileId;
    private UUID photographerUserId;
    private UUID inquiryId;
    private PhotographerSummary photographer;
    private CompletedInquiry engagement;

    @BeforeEach
    void setUp() {
        customerId            = UUID.randomUUID();
        photographerProfileId = UUID.randomUUID();
        photographerUserId    = UUID.randomUUID();
        inquiryId             = UUID.randomUUID();

        photographer = new PhotographerSummary(
                photographerProfileId, photographerUserId, "Alice Camera");
        engagement = new CompletedInquiry(
                inquiryId, customerId, photographerProfileId, photographerUserId);
    }

    // ── createReview ──────────────────────────────────────────────────────────

    @Test
    void createReview_happyPath_persistsAndReturns() {
        CreateReviewRequest request = new CreateReviewRequest(photographerProfileId, 5, "Lovely shoot");
        given(repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId))
                .willReturn(false);
        given(photographerClient.getPhotographer(photographerProfileId)).willReturn(photographer);
        given(inquiryClient.findCompletedEngagement(customerId, photographerProfileId)).willReturn(engagement);

        Review saved = stubReview((short) 5, "Lovely shoot");
        given(repository.save(any(Review.class))).willReturn(saved);
        given(mapper.toResponse(saved)).willReturn(new ReviewResponse(
                saved.getId(), customerId, photographerProfileId, photographerUserId,
                inquiryId, 5, "Lovely shoot", saved.getCreatedAt(), saved.getUpdatedAt()));

        ReviewResponse response = service.createReview(customerId, request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.body()).isEqualTo("Lovely shoot");
        assertThat(response.inquiryId()).isEqualTo(inquiryId);
        then(repository).should().save(any(Review.class));
    }

    @Test
    void createReview_duplicate_throws409WithoutFeign() {
        CreateReviewRequest request = new CreateReviewRequest(photographerProfileId, 4, null);
        given(repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId))
                .willReturn(true);

        assertThatThrownBy(() -> service.createReview(customerId, request))
                .isInstanceOf(DuplicateReviewException.class);

        // The dedup check is BEFORE the Feign hops — don't burn a network call
        // just to discover what we already knew.
        then(photographerClient).shouldHaveNoInteractions();
        then(inquiryClient).shouldHaveNoInteractions();
        then(repository).should(never()).save(any());
    }

    @Test
    void createReview_noCompletedBooking_throws403() {
        CreateReviewRequest request = new CreateReviewRequest(photographerProfileId, 5, "n/a");
        given(repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId))
                .willReturn(false);
        given(photographerClient.getPhotographer(photographerProfileId)).willReturn(photographer);
        given(inquiryClient.findCompletedEngagement(customerId, photographerProfileId))
                .willThrow(feignNotFound());

        assertThatThrownBy(() -> service.createReview(customerId, request))
                .isInstanceOf(NoCompletedBookingException.class);
        then(repository).should(never()).save(any());
    }

    @Test
    void createReview_photographerMissing_throws400() {
        CreateReviewRequest request = new CreateReviewRequest(photographerProfileId, 3, null);
        given(repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId))
                .willReturn(false);
        given(photographerClient.getPhotographer(photographerProfileId)).willThrow(feignNotFound());

        assertThatThrownBy(() -> service.createReview(customerId, request))
                .isInstanceOf(PhotographerNotFoundException.class);
        then(inquiryClient).shouldHaveNoInteractions();
        then(repository).should(never()).save(any());
    }

    @Test
    void createReview_photographerServiceDown_throws503() {
        CreateReviewRequest request = new CreateReviewRequest(photographerProfileId, 3, null);
        given(repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId))
                .willReturn(false);
        given(photographerClient.getPhotographer(photographerProfileId))
                .willThrow(feignServerError());

        assertThatThrownBy(() -> service.createReview(customerId, request))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessageContaining("photographer-service");
    }

    @Test
    void createReview_customerServiceDown_throws503() {
        CreateReviewRequest request = new CreateReviewRequest(photographerProfileId, 4, "ok");
        given(repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId))
                .willReturn(false);
        given(photographerClient.getPhotographer(photographerProfileId)).willReturn(photographer);
        given(inquiryClient.findCompletedEngagement(customerId, photographerProfileId))
                .willThrow(feignServerError());

        assertThatThrownBy(() -> service.createReview(customerId, request))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessageContaining("customer-service");
    }

    // ── summarise ─────────────────────────────────────────────────────────────

    @Test
    void summarise_existing_returnsAverageAndCount() {
        given(repository.summarise(photographerProfileId)).willReturn(summaryOf(4.4, 5));

        ReviewSummaryResponse response = service.summarise(photographerProfileId);

        assertThat(response.averageRating()).isEqualTo(4.4);
        assertThat(response.reviewCount()).isEqualTo(5);
    }

    @Test
    void summarise_noReviews_returnsZeros() {
        // JPQL AVG returns NULL when the table is empty; service flattens that
        // to 0.0/0 so the front-end never has to special-case a 404.
        given(repository.summarise(photographerProfileId)).willReturn(summaryOf(null, 0));

        ReviewSummaryResponse response = service.summarise(photographerProfileId);

        assertThat(response.averageRating()).isZero();
        assertThat(response.reviewCount()).isZero();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Review stubReview(short rating, String body) {
        Review r = new Review();
        r.setCustomerId(customerId);
        r.setPhotographerProfileId(photographerProfileId);
        r.setPhotographerUserId(photographerUserId);
        r.setInquiryId(inquiryId);
        r.setRating(rating);
        r.setBody(body);
        // @PrePersist doesn't fire outside a real EntityManager — stamp manually.
        Instant now = Instant.now();
        reflectSet(r, "id", UUID.randomUUID());
        reflectSet(r, "createdAt", now);
        reflectSet(r, "updatedAt", now);
        return r;
    }

    private static void reflectSet(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ReviewSummary summaryOf(Double avg, long count) {
        return new ReviewSummary() {
            @Override public Double getAverageRating() { return avg; }
            @Override public long getReviewCount() { return count; }
        };
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
