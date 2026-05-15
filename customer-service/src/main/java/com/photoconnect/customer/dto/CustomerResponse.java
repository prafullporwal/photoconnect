package com.photoconnect.customer.dto;

import com.photoconnect.customer.domain.ContactMethod;

import java.time.Instant;
import java.util.UUID;

/** Response shape for a customer profile. */
public record CustomerResponse(
        UUID id,
        UUID userId,
        String displayName,
        String location,
        String phoneNumber,
        ContactMethod preferredContactMethod,
        Instant createdAt,
        Instant updatedAt
) {}
