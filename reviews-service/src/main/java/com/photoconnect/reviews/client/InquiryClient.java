package com.photoconnect.reviews.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign client to customer-service for the "completed booking" predicate.
 *
 * <p>Calls {@code GET /internal/v1/inquiries/completed?customerId=&photographerProfileId=}
 * which returns 200 + payload when at least one COMPLETED inquiry exists
 * between this customer and photographer, or 404 otherwise.</p>
 *
 * <p>This is the only contract reviews-service knows about. Customer-service
 * is free to change inquiry-status names tomorrow; as long as it keeps the
 * "do they have a completed engagement?" semantics on this endpoint, we are
 * insulated from those changes.</p>
 */
@FeignClient(name = "customer-service")
public interface InquiryClient {

    @GetMapping("/internal/v1/inquiries/completed")
    CompletedInquiry findCompletedEngagement(
            @RequestParam("customerId") UUID customerId,
            @RequestParam("photographerProfileId") UUID photographerProfileId);
}
