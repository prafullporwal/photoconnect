package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when an authenticated user is neither the customer who created an
 * inquiry nor the photographer it was sent to.
 */
public class InquiryAccessDeniedException extends CustomerDomainException {
    public InquiryAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "You are not a participant in this inquiry");
    }
}
