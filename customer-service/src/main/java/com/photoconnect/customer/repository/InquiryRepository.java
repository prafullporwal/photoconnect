package com.photoconnect.customer.repository;

import com.photoconnect.customer.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    /** "Inquiries I sent" — used by the customer's own inbox. */
    List<Inquiry> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /** "Inquiries I received" — used by the photographer's inbox. */
    List<Inquiry> findByPhotographerUserIdOrderByCreatedAtDesc(UUID photographerUserId);
}
