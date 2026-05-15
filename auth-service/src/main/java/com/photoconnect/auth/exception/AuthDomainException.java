package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for business-rule exceptions in auth-service. Each subclass
 * declares its HTTP status — {@code GlobalExceptionHandler} reads it directly
 * so handlers stay short.
 */
public abstract class AuthDomainException extends RuntimeException {

    private final HttpStatus status;

    protected AuthDomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
