package com.photoconnect.reviews.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Raised when a customer tries to review a photographer they have no
 * COMPLETED booking with. Returns 403 — the request is authenticated and
 * well-formed, but the policy forbids it.
 */
public class NoCompletedBookingException extends ReviewDomainException {
    public NoCompletedBookingException(UUID customerId, UUID photographerProfileId) {
        super(HttpStatus.FORBIDDEN,
                "Customer " + customerId + " has no completed booking with photographer "
                        + photographerProfileId + " — reviews are only allowed after a completed engagement");
    }
}
