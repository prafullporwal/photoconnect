package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Raised when the Feign call to photographer-service returns 404 — the
 * client tried to create an inquiry against a photographer that doesn't exist.
 */
public class PhotographerNotFoundException extends CustomerDomainException {
    public PhotographerNotFoundException(UUID photographerId) {
        super(HttpStatus.BAD_REQUEST,
                "No photographer found with id " + photographerId);
    }
}
