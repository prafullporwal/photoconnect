package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when photographer-service is unreachable or returns 5xx after retries.
 *
 * <p>Returns 503 to the caller so the front-end can show "try again shortly"
 * rather than a misleading 5xx that looks like a bug in our service.</p>
 */
public class PhotographerServiceUnavailableException extends CustomerDomainException {
    public PhotographerServiceUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "Photographer service is currently unavailable: " + cause.getMessage());
        initCause(cause);
    }
}
