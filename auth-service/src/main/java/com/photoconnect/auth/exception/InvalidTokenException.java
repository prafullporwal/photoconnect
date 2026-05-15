package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AuthDomainException {
    public InvalidTokenException(String detail) {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired token: " + detail);
    }
}
