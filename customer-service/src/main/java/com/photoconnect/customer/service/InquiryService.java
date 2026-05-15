package com.photoconnect.customer.service;

import com.photoconnect.customer.client.PhotographerClient;
import com.photoconnect.customer.client.PhotographerSummary;
import com.photoconnect.customer.domain.Inquiry;
import com.photoconnect.customer.domain.InquiryStatus;
import com.photoconnect.customer.dto.CreateInquiryRequest;
import com.photoconnect.customer.dto.InquiryResponse;
import com.photoconnect.customer.exception.InquiryAccessDeniedException;
import com.photoconnect.customer.exception.InquiryNotFoundException;
import com.photoconnect.customer.exception.PhotographerNotFoundException;
import com.photoconnect.customer.exception.PhotographerServiceUnavailableException;
import com.photoconnect.customer.mapper.InquiryMapper;
import com.photoconnect.customer.repository.InquiryRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core business logic for inquiries — including the cross-service call to
 * photographer-service when a new inquiry is created.
 *
 * <h2>The Feign call</h2>
 * <p>Before persisting an inquiry, we ask photographer-service "does this
 * photographer exist?" via {@link PhotographerClient}. Three things come out
 * of that single Feign call:</p>
 * <ul>
 *   <li><b>Existence check</b> — a 404 means the customer pointed at a
 *       non-existent profile, which becomes a 400 to our caller (their input
 *       was bad, not our service).</li>
 *   <li><b>{@code photographerUserId}</b> — denormalised onto the inquiry row
 *       so the photographer can later query their inbox by {@code X-User-Id}
 *       without joining across services.</li>
 *   <li><b>Health-check signal</b> — if the Feign call fails with a 5xx or a
 *       network error after retries, we return 503 to the caller (it's a
 *       platform problem, not a client problem).</li>
 * </ul>
 *
 * <p>This is the canonical "verify-then-write" pattern for cross-service
 * writes. The denormalisation trades a small consistency window (if the
 * photographer is deleted right after we capture their userId, we have stale
 * data) for query-time independence (we can list inquiries without calling
 * photographer-service on every read).</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository repository;
    private final InquiryMapper mapper;
    private final PhotographerClient photographerClient;

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Create an inquiry against a verified photographer.
     *
     * @throws PhotographerNotFoundException        if the photographer doesn't exist (400)
     * @throws PhotographerServiceUnavailableException if Feign call fails after retries (503)
     */
    public InquiryResponse createInquiry(UUID customerId, CreateInquiryRequest request) {

        // ── Cross-service call: resolve photographer or fail loudly ───────────
        PhotographerSummary photographer = lookupPhotographer(request.photographerProfileId());

        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerId(customerId);
        inquiry.setPhotographerProfileId(photographer.id());
        inquiry.setPhotographerUserId(photographer.userId());    // ← denormalised from Feign
        inquiry.setEventDate(request.eventDate());
        inquiry.setEventType(request.eventType());
        inquiry.setLocation(request.location());
        inquiry.setBudget(request.budget());
        inquiry.setMessage(request.message());
        inquiry.setStatus(InquiryStatus.NEW);

        Inquiry saved = repository.save(inquiry);
        log.info("Created inquiry {} from customer {} to photographer {} (user {})",
                saved.getId(), customerId, photographer.id(), photographer.userId());
        return mapper.toResponse(saved);
    }

    /**
     * Update an inquiry's status.
     *
     * <p>Authorization happens here, not in the controller: only the customer
     * who created the inquiry OR the photographer it was sent to may change
     * its status.</p>
     */
    public InquiryResponse updateStatus(UUID inquiryId, UUID callerUserId, InquiryStatus newStatus) {
        Inquiry inquiry = repository.findById(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException(inquiryId));

        if (!isParticipant(inquiry, callerUserId)) {
            throw new InquiryAccessDeniedException();
        }

        inquiry.setStatus(newStatus);
        return mapper.toResponse(repository.save(inquiry));
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(UUID inquiryId, UUID callerUserId) {
        Inquiry inquiry = repository.findById(inquiryId)
                .orElseThrow(() -> new InquiryNotFoundException(inquiryId));

        if (!isParticipant(inquiry, callerUserId)) {
            throw new InquiryAccessDeniedException();
        }
        return mapper.toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listMyInquiries(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listReceivedInquiries(UUID photographerUserId) {
        return repository.findByPhotographerUserIdOrderByCreatedAtDesc(photographerUserId)
                .stream().map(mapper::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PhotographerSummary lookupPhotographer(UUID photographerProfileId) {
        try {
            return photographerClient.getPhotographer(photographerProfileId);
        } catch (FeignException.NotFound e) {
            // photographer-service explicitly told us 404 — customer's input was wrong
            throw new PhotographerNotFoundException(photographerProfileId);
        } catch (FeignException e) {
            // 5xx, connection refused, timeout after retries → platform problem
            log.warn("Feign call to photographer-service failed: status={}, message={}",
                    e.status(), e.getMessage());
            throw new PhotographerServiceUnavailableException(e);
        }
    }

    private static boolean isParticipant(Inquiry inquiry, UUID callerUserId) {
        return inquiry.getCustomerId().equals(callerUserId)
                || inquiry.getPhotographerUserId().equals(callerUserId);
    }
}
