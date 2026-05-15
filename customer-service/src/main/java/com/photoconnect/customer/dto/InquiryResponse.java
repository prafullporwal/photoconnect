package com.photoconnect.customer.dto;

import com.photoconnect.customer.domain.InquiryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Response shape for an inquiry. */
public record InquiryResponse(
        UUID id,
        UUID customerId,
        UUID photographerProfileId,
        UUID photographerUserId,
        LocalDate eventDate,
        String eventType,
        String location,
        BigDecimal budget,
        String message,
        InquiryStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
