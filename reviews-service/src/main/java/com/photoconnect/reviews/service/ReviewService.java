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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core business logic for reviews.
 *
 * <h2>The booking precondition</h2>
 * <p>"Only allowed after a completed booking" is the headline rule. We enforce
 * it by calling customer-service's internal endpoint before persisting. The
 * Feign call returns the {@link CompletedInquiry} that authorised the review;
 * its {@code inquiryId} is stamped onto the review row as an audit trail.</p>
 *
 * <h2>Two integrity layers for "one review per pair"</h2>
 * <ol>
 *   <li><b>Application check</b> ({@code existsByCustomerIdAndPhotographerProfileId})
 *       — friendly, runs before the verify call so we don't burn a Feign hop
 *       on a duplicate.</li>
 *   <li><b>DB UNIQUE constraint</b> — wins any race the application check
 *       can't see. The unique-violation surfaces as a
 *       {@code DataIntegrityViolationException} which
 *       {@link com.photoconnect.reviews.exception.GlobalExceptionHandler}
 *       maps to 409, matching the application-level path.</li>
 * </ol>
 *
 * <p>That's the standard pattern when an integrity rule is both a friendly
 * service-layer check AND a hard DB constraint: the DB is the source of truth,
 * the app catches the violation and renders it as the same 409.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    private final InquiryClient inquiryClient;
    private final PhotographerClient photographerClient;

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Persist a new review for an authenticated customer.
     *
     * @throws DuplicateReviewException             if a review already exists for the (customer, photographer)
     * @throws NoCompletedBookingException          if customer has no COMPLETED inquiry with the photographer
     * @throws PhotographerNotFoundException        if photographer-service returns 404
     * @throws DownstreamServiceUnavailableException if a downstream Feign call fails after retries
     */
    public ReviewResponse createReview(UUID customerId, CreateReviewRequest request) {

        UUID photographerProfileId = request.photographerProfileId();

        // ── 1. Cheap dedup check before any Feign work ───────────────────────
        if (repository.existsByCustomerIdAndPhotographerProfileId(customerId, photographerProfileId)) {
            throw new DuplicateReviewException(customerId, photographerProfileId);
        }

        // ── 2. Validate photographer exists + capture their userId ───────────
        PhotographerSummary photographer = lookupPhotographer(photographerProfileId);

        // ── 3. Enforce the policy: a COMPLETED inquiry must exist ────────────
        CompletedInquiry engagement = requireCompletedEngagement(customerId, photographerProfileId);

        // ── 4. Persist ───────────────────────────────────────────────────────
        Review review = new Review();
        review.setCustomerId(customerId);
        review.setPhotographerProfileId(photographer.id());
        review.setPhotographerUserId(photographer.userId());
        review.setInquiryId(engagement.inquiryId());
        review.setRating(request.rating().shortValue());
        review.setBody(request.body());

        Review saved = repository.save(review);
        log.info("Created review {} by customer {} for photographer {} (rating={}, inquiry={})",
                saved.getId(), customerId, photographer.id(), saved.getRating(), engagement.inquiryId());
        return mapper.toResponse(saved);
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForPhotographer(UUID photographerProfileId) {
        return repository.findByPhotographerProfileIdOrderByCreatedAtDesc(photographerProfileId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listMine(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse summarise(UUID photographerProfileId) {
        ReviewSummary summary = repository.summarise(photographerProfileId);
        // AVG returns NULL when there are no rows; flatten to 0.0/0 for the API.
        double avg = summary == null || summary.getAverageRating() == null
                ? 0.0
                : summary.getAverageRating();
        long count = summary == null ? 0L : summary.getReviewCount();
        return new ReviewSummaryResponse(photographerProfileId, avg, count);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PhotographerSummary lookupPhotographer(UUID photographerProfileId) {
        try {
            return photographerClient.getPhotographer(photographerProfileId);
        } catch (FeignException.NotFound e) {
            throw new PhotographerNotFoundException(photographerProfileId);
        } catch (FeignException e) {
            log.warn("Feign call to photographer-service failed: status={}, message={}",
                    e.status(), e.getMessage());
            throw new DownstreamServiceUnavailableException("photographer-service", e);
        }
    }

    private CompletedInquiry requireCompletedEngagement(UUID customerId, UUID photographerProfileId) {
        try {
            return inquiryClient.findCompletedEngagement(customerId, photographerProfileId);
        } catch (FeignException.NotFound e) {
            throw new NoCompletedBookingException(customerId, photographerProfileId);
        } catch (FeignException e) {
            log.warn("Feign call to customer-service failed: status={}, message={}",
                    e.status(), e.getMessage());
            throw new DownstreamServiceUnavailableException("customer-service", e);
        }
    }
}
