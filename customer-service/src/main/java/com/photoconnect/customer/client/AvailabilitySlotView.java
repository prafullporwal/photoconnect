package com.photoconnect.customer.client;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tolerant-reader projection of photographer-service's availability slot.
 *
 * <p>Only the fields customer-service actually needs to perform its
 * "is the requested date available?" check are listed. Jackson will fill the
 * matched fields and ignore the rest — adding fields server-side never breaks
 * this consumer.</p>
 */
public record AvailabilitySlotView(
        UUID id,
        LocalDate availableDate
) {}
