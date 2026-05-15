package com.photoconnect.customer.client;

import java.util.UUID;

/**
 * Slim view of a photographer profile — only the fields we need from the Feign call.
 *
 * <p>Jackson will fill {@code id}, {@code userId}, and {@code displayName}
 * from photographer-service's response and ignore all other fields by default.
 * This is a deliberate <em>tolerant reader</em>: photographer-service can add
 * fields to its response without breaking us.</p>
 */
public record PhotographerSummary(
        UUID id,
        UUID userId,
        String displayName
) {}
