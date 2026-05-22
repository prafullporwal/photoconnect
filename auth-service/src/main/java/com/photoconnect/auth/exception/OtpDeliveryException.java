package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the outbound SMS provider fails after retries. Maps to HTTP 502
 * so the caller can distinguish "your input was fine, our SMS gateway is down"
 * from a client-side validation error (400) or wrong code (401).
 */
public class OtpDeliveryException extends AuthDomainException {
    public OtpDeliveryException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}
