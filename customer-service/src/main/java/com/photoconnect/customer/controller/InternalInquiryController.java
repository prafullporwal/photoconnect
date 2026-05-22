package com.photoconnect.customer.controller;

import com.photoconnect.customer.dto.CompletedInquiryResponse;
import com.photoconnect.customer.service.InternalInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service endpoints. NOT routed through api-gateway — callers hit
 * customer-service directly via Eureka with a service JWT in the
 * {@code Authorization} header.
 *
 * <h2>Authorisation</h2>
 * <ul>
 *   <li>{@code hasRole('SERVICE')} — only callers authenticated via
 *       {@code ServiceTokenAuthenticationFilter}. User access tokens are
 *       rejected.</li>
 *   <li>{@code hasAuthority('SCOPE_inquiry-read')} — the scope on the caller's
 *       token must include this value. Currently only reviews-service is
 *       granted it via {@code auth-service.properties}.</li>
 * </ul>
 */
@Tag(name = "internal", description = "Service-to-service endpoints (auth: service JWT)")
@RestController
@RequestMapping("/internal/v1/inquiries")
@RequiredArgsConstructor
public class InternalInquiryController {

    private final InternalInquiryService service;

    @Operation(summary = "Find the most-recent COMPLETED inquiry between a customer and photographer (service-to-service)")
    @GetMapping("/completed")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('SCOPE_inquiry-read')")
    public ResponseEntity<CompletedInquiryResponse> findCompleted(
            @RequestParam("customerId") UUID customerId,
            @RequestParam("photographerProfileId") UUID photographerProfileId) {
        return ResponseEntity.ok(service.findCompletedEngagement(customerId, photographerProfileId));
    }
}
