package com.photoconnect.reviews.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Raised when a customer tries to review the same photographer twice.
 * 409 Conflict — the resource (their one review) already exists.
 */
public class DuplicateReviewException extends ReviewDomainException {
    public DuplicateReviewException(UUID customerId, UUID photographerProfileId) {
        super(HttpStatus.CONFLICT,
                "Customer " + customerId + " has already reviewed photographer "
                        + photographerProfileId);
    }
}
