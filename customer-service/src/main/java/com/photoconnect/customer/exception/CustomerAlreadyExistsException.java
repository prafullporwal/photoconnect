package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CustomerAlreadyExistsException extends CustomerDomainException {
    public CustomerAlreadyExistsException(UUID userId) {
        super(HttpStatus.CONFLICT, "A customer profile already exists for user " + userId);
    }
}
