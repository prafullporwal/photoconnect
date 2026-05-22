package com.photoconnect.customer.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Raised when an inquiry is submitted for a date the photographer has not
 * marked as available.
 *
 * <p>Maps to HTTP 400 because the customer's input is what's wrong — the
 * frontend should refresh the date picker and surface this message.</p>
 */
public class DateUnavailableException extends CustomerDomainException {

    public DateUnavailableException(LocalDate requestedDate) {
        super(HttpStatus.BAD_REQUEST,
                "The selected date (" + requestedDate
                        + ") is no longer in the photographer's available calendar. "
                        + "Please pick a different date.");
    }
}
