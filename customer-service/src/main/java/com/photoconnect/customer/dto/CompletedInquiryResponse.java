package com.photoconnect.customer.dto;

import java.util.UUID;

/**
 * Slim view of a completed inquiry — only the fields reviews-service needs to
 * stamp an audit pointer onto a review row. Deliberately minimal so we don't
 * leak more inquiry data than the consumer needs.
 *
 * @param inquiryId              PK of the completed inquiry
 * @param customerId             auth-service userId of the customer
 * @param photographerProfileId  PhotographerProfile PK
 * @param photographerUserId     auth-service userId of the photographer
 */
public record CompletedInquiryResponse(
        UUID inquiryId,
        UUID customerId,
        UUID photographerProfileId,
        UUID photographerUserId
) {}
