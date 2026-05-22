package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

/** A user with this phone already exists and the caller asked to register, not log in. */
public class PhoneAlreadyRegisteredException extends AuthDomainException {
    public PhoneAlreadyRegisteredException(String phone) {
        super(HttpStatus.CONFLICT, "Phone already registered: " + phone);
    }
}
