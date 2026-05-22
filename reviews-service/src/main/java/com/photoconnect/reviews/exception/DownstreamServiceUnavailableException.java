package com.photoconnect.reviews.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a downstream service (customer-service or photographer-service)
 * is unreachable or returns 5xx after retries. Surfaces as 503 to the caller
 * so the front-end can show "try again shortly" rather than implying a bug in
 * reviews-service itself.
 */
public class DownstreamServiceUnavailableException extends ReviewDomainException {
    public DownstreamServiceUnavailableException(String serviceName, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                serviceName + " is currently unavailable: " + cause.getMessage());
        initCause(cause);
    }
}
