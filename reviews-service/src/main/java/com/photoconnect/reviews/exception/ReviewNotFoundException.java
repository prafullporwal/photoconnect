package com.photoconnect.reviews.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ReviewNotFoundException extends ReviewDomainException {
    public ReviewNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Review not found: " + id);
    }
}
