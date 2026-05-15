package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class InquiryNotFoundException extends CustomerDomainException {
    public InquiryNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Inquiry not found: " + id);
    }
}
