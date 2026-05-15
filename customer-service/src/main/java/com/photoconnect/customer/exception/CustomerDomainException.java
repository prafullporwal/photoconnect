package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

/** Base class so {@code GlobalExceptionHandler} can map domain errors to HTTP status in one go. */
public abstract class CustomerDomainException extends RuntimeException {

    private final HttpStatus status;

    protected CustomerDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
