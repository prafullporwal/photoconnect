package com.photoconnect.photographer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Bulk-add request for availability slots.
 *
 * <p>Bulk by design — the photographer's calendar UI lets them click multiple
 * days and "Save" once. Sending each date as its own request would be N
 * round-trips for the same operation. The service treats the list as a "set
 * union": existing dates are silently skipped (idempotent), new ones inserted.</p>
 *
 * @param dates non-empty list of dates to mark available. Past dates are
 *              filtered server-side rather than rejected — calendars often
 *              span month boundaries and the UI shouldn't have to defend.
 * @param note  optional note applied to every newly-created slot. Use the
 *              per-slot edit endpoint (Phase 2) for date-specific notes.
 */
public record AddAvailabilityRequest(
        @NotNull
        @NotEmpty(message = "At least one date is required")
        List<LocalDate> dates,

        @Size(max = 200, message = "Note must be 200 characters or fewer")
        String note
) {}
