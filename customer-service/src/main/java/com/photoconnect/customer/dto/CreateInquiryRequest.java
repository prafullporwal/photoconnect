package com.photoconnect.customer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body for {@code POST /api/v1/inquiries}.
 *
 * <p>The customer only sends the photographer's <em>profile</em> ID. The
 * service calls photographer-service via Feign to resolve the matching
 * {@code userId} (and to verify the photographer actually exists).</p>
 */
public record CreateInquiryRequest(

        @NotNull(message = "Photographer ID is required")
        UUID photographerProfileId,

        @NotNull(message = "Event date is required")
        @FutureOrPresent(message = "Event date cannot be in the past")
        LocalDate eventDate,

        @NotBlank @Size(max = 50) String eventType,

        @Size(max = 200) String location,

        @DecimalMin(value = "0.00", message = "Budget cannot be negative")
        @Digits(integer = 8, fraction = 2)
        BigDecimal budget,

        @NotBlank @Size(max = 5000) String message
) {}
