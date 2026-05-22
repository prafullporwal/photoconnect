package com.photoconnect.customer.client;

import java.util.UUID;

/**
 * Slim view of a photographer profile — only the fields we need from the Feign call.
 *
 * <p>Jackson will fill these fields from photographer-service's full response
 * and ignore all other fields by default. This is a deliberate
 * <em>tolerant reader</em>: photographer-service can add fields to its response
 * without breaking us. Absent fields (e.g. on an older service version) default
 * to {@code null} because all components are reference types.</p>
 */
public record PhotographerSummary(
        UUID id,
        UUID userId,
        String displayName,
        String location,
        java.math.BigDecimal pricePerHour
) {}
