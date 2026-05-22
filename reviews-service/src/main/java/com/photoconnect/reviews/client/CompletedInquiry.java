package com.photoconnect.reviews.client;

import java.util.UUID;

/**
 * Slim view of the most-recent COMPLETED inquiry between a customer and a
 * photographer, returned by customer-service's internal endpoint. The
 * {@code inquiryId} becomes the audit pointer on the review row.
 */
public record CompletedInquiry(
        UUID inquiryId,
        UUID customerId,
        UUID photographerProfileId,
        UUID photographerUserId
) {}
