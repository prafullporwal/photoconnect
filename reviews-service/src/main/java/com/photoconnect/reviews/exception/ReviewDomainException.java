package com.photoconnect.reviews.exception;

import org.springframework.http.HttpStatus;

/** Base class so {@code GlobalExceptionHandler} can map domain errors to HTTP status in one go. */
public abstract class ReviewDomainException extends RuntimeException {

    private final HttpStatus status;

    protected ReviewDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
