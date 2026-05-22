package com.photoconnect.customer.service;

import com.photoconnect.customer.domain.Inquiry;
import com.photoconnect.customer.domain.InquiryStatus;
import com.photoconnect.customer.dto.CompletedInquiryResponse;
import com.photoconnect.customer.exception.InquiryNotFoundException;
import com.photoconnect.customer.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Read-only service for the {@code /internal/v1/inquiries/**} surface used by
 * other services (currently reviews-service).
 *
 * <p>Kept separate from {@link InquiryService} so the user-facing API and the
 * service-to-service API don't accidentally share method signatures. They have
 * different authorisation models and different evolution timelines.</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InternalInquiryService {

    private final InquiryRepository repository;

    /**
     * Find the most-recent COMPLETED inquiry between this customer and
     * photographer-profile pair. Throws {@link InquiryNotFoundException} (→ 404)
     * when there isn't one — the caller (reviews-service) treats 404 as
     * "no completed booking, refuse the review".
     */
    public CompletedInquiryResponse findCompletedEngagement(UUID customerId, UUID photographerProfileId) {
        Inquiry inquiry = repository
                .findFirstByCustomerIdAndPhotographerProfileIdAndStatusOrderByUpdatedAtDesc(
                        customerId, photographerProfileId, InquiryStatus.COMPLETED)
                .orElseThrow(() -> new InquiryNotFoundException(photographerProfileId));

        return new CompletedInquiryResponse(
                inquiry.getId(),
                inquiry.getCustomerId(),
                inquiry.getPhotographerProfileId(),
                inquiry.getPhotographerUserId());
    }
}
