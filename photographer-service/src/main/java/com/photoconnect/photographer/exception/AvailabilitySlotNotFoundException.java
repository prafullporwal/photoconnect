package com.photoconnect.photographer.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when a slot delete targets a non-existent or unowned slot.
 *
 * <p>The owner-scoped repository delete returns 0 rows in both cases — we
 * deliberately don't distinguish "doesn't exist" from "exists but isn't yours"
 * because that would leak information about other photographers' calendars.</p>
 */
public class AvailabilitySlotNotFoundException extends PhotographerDomainException {

    public AvailabilitySlotNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Availability slot not found: " + id);
    }
}
