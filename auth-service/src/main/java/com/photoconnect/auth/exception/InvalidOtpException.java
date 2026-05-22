package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the supplied OTP is wrong, expired, or has no attempts left. */
public class InvalidOtpException extends AuthDomainException {
    public InvalidOtpException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
