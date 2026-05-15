package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends AuthDomainException {
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "Email already registered: " + email);
    }
}
