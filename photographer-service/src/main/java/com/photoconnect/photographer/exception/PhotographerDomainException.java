package com.photoconnect.photographer.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain exceptions in photographer-service.
 * Carries an HTTP status so {@link GlobalExceptionHandler} can return the
 * right code without a giant if/else chain.
 */
public abstract class PhotographerDomainException extends RuntimeException {

    private final HttpStatus status;

    protected PhotographerDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
