package com.photoconnect.photographer.exception;

import org.springframework.http.HttpStatus;

/** The uploaded file is missing, empty, or has an unsupported MIME type. */
public class InvalidPortfolioFileException extends PhotographerDomainException {
    public InvalidPortfolioFileException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
