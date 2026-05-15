package com.photoconnect.photographer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/** Thrown when a requested photographer profile does not exist. */
public class ProfileNotFoundException extends PhotographerDomainException {

    public ProfileNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND,
                "Photographer profile not found: " + id);
    }
}
