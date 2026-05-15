package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CustomerNotFoundException extends CustomerDomainException {
    public CustomerNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Customer profile not found: " + id);
    }
}
