package com.photoconnect.photographer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/** Thrown when a user tries to create a second photographer profile. */
public class ProfileAlreadyExistsException extends PhotographerDomainException {

    public ProfileAlreadyExistsException(UUID userId) {
        super(HttpStatus.CONFLICT,
                "A photographer profile already exists for user " + userId);
    }
}
