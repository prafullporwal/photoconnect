package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a service-token request fails authentication — either the
 * {@code clientId} is unknown, or the presented {@code clientSecret} does not
 * match the registered BCrypt hash. We deliberately return a single opaque
 * message: leaking "unknown client" vs. "wrong secret" gives an attacker a
 * way to enumerate valid client IDs.
 */
public class InvalidServiceClientException extends AuthDomainException {
    public InvalidServiceClientException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid client credentials");
    }
}
