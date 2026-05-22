package com.photoconnect.customer.repository;

import com.photoconnect.customer.domain.Inquiry;
import com.photoconnect.customer.domain.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    /** "Inquiries I sent" — used by the customer's own inbox. */
    List<Inquiry> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /** "Inquiries I received" — used by the photographer's inbox. */
    List<Inquiry> findByPhotographerUserIdOrderByCreatedAtDesc(UUID photographerUserId);

    /**
     * Used by reviews-service to verify a "completed booking" exists between
     * a customer and a photographer. Returns the most-recently completed
     * inquiry so the caller can stamp its id onto the resulting review.
     */
    Optional<Inquiry> findFirstByCustomerIdAndPhotographerProfileIdAndStatusOrderByUpdatedAtDesc(
            UUID customerId, UUID photographerProfileId, InquiryStatus status);
}
