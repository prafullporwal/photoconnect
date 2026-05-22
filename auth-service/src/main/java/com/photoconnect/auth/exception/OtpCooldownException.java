package com.photoconnect.auth.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a re-send is requested before the cooldown window has elapsed. */
public class OtpCooldownException extends AuthDomainException {
    public OtpCooldownException(long secondsRemaining) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Please wait " + secondsRemaining + "s before requesting another code");
    }
}
