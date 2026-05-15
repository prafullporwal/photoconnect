package com.photoconnect.photographer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/** The requested portfolio item doesn't exist (or isn't owned by the caller). */
public class PortfolioItemNotFoundException extends PhotographerDomainException {
    public PortfolioItemNotFoundException(UUID itemId) {
        super(HttpStatus.NOT_FOUND, "Portfolio item not found: " + itemId);
    }
}
