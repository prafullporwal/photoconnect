package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthDomainException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
