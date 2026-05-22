package com.photoconnect.photographer.controller;

import com.photoconnect.photographer.dto.AvailabilitySlotResponse;
import com.photoconnect.photographer.dto.FeedItemResponse;
import com.photoconnect.photographer.dto.PhotographerProfileResponse;
import com.photoconnect.photographer.service.AvailabilityService;
import com.photoconnect.photographer.service.PhotographerService;
import com.photoconnect.photographer.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Service-to-service endpoints. NOT routed through api-gateway — callers hit
 * photographer-service directly via Eureka with a service JWT in the
 * {@code Authorization} header.
 *
 * <h2>Path convention</h2>
 * <p>Everything under {@code /internal/**} is for other services only. The
 * gateway's route table doesn't include this prefix, so external clients
 * cannot reach it even if they discover the URL.</p>
 *
 * <h2>Authorization</h2>
 * <p>Two checks, defence-in-depth:</p>
 * <ul>
 *   <li>{@code hasRole('SERVICE')} — only callers authenticated via
 *       {@link com.photoconnect.photographer.security.ServiceTokenAuthenticationFilter}
 *       reach here. User access tokens, even legitimate ones, are rejected.</li>
 *   <li>{@code hasAuthority('SCOPE_photographer-read')} — the caller's token
 *       must have been issued for this scope.</li>
 * </ul>
 */
@Tag(name = "internal", description = "Service-to-service endpoints (auth: service JWT)")
@RestController
@RequestMapping("/internal/v1/photographers")
@RequiredArgsConstructor
public class InternalPhotographerController {

    private final PhotographerService service;
    private final AvailabilityService availabilityService;
    private final PortfolioService portfolioService;

    @Operation(summary = "Fetch a photographer profile (service-to-service)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('SCOPE_photographer-read')")
    public ResponseEntity<PhotographerProfileResponse> getPhotographer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getProfileById(id));
    }

    @Operation(summary = "Fetch a photographer's availability calendar (service-to-service)")
    @GetMapping("/{id}/availability")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('SCOPE_photographer-read')")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(availabilityService.listForProfile(id));
    }

    @Operation(summary = "Fetch a single portfolio item as a feed item (service-to-service)")
    @GetMapping("/portfolio/{itemId}")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('SCOPE_photographer-read')")
    public ResponseEntity<FeedItemResponse> getPortfolioItem(@PathVariable UUID itemId) {
        return ResponseEntity.ok(portfolioService.getAsFeedItem(itemId));
    }
}
