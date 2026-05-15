package com.photoconnect.photographer.controller;

import com.photoconnect.photographer.domain.MediaType;
import com.photoconnect.photographer.dto.FeedItemResponse;
import com.photoconnect.photographer.dto.PortfolioItemResponse;
import com.photoconnect.photographer.security.GatewayPrincipal;
import com.photoconnect.photographer.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for managing and viewing portfolio media.
 *
 * <h2>Access matrix</h2>
 * <table>
 *   <tr><th>Endpoint</th><th>Photographer (self)</th><th>Customer / anon</th><th>Other photographer</th></tr>
 *   <tr><td>{@code POST /me/portfolio}</td><td>200</td><td>403</td><td>403</td></tr>
 *   <tr><td>{@code GET  /me/portfolio}</td><td>200</td><td>403</td><td>403</td></tr>
 *   <tr><td>{@code DELETE /me/portfolio/{id}}</td><td>200</td><td>403</td><td>403</td></tr>
 *   <tr><td>{@code GET /{profileId}/portfolio}</td><td>—</td><td>200</td><td>403 (blocked from browsing)</td></tr>
 * </table>
 *
 * <p>The customer-facing gallery endpoint reuses the same
 * {@code !hasRole('PHOTOGRAPHER')} rule as the rest of the browse — a
 * photographer can't peek at competitors' portfolios.</p>
 */
@RestController
@RequestMapping("/api/v1/photographers")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService service;

    // ── Owner endpoints (PHOTOGRAPHER only — operates on "me") ────────────────

    /**
     * Upload a new sample asset. Multipart body with three parts:
     * {@code file}, {@code mediaType} (IMAGE/VIDEO/REEL), {@code category}.
     */
    @PostMapping(value = "/me/portfolio", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<PortfolioItemResponse> upload(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType,
            @RequestParam("category") String category) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.upload(caller.userId(), file, mediaType, category));
    }

    /** List my own portfolio items (no filters at this layer — SPA filters client-side). */
    @GetMapping("/me/portfolio")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<PortfolioItemResponse>> listMine(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.listMine(caller.userId()));
    }

    @DeleteMapping("/me/portfolio/{itemId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID itemId) {
        service.delete(caller.userId(), itemId);
        return ResponseEntity.noContent().build();
    }

    // ── Customer-facing gallery + feed ────────────────────────────────────────

    /**
     * Marketplace feed — newest portfolio media across all available photographers.
     * Drives the SPA's main browse page (Pinterest/Instagram-style explore).
     *
     * <p>Path is {@code /feed} which Spring MVC resolves as an exact match
     * before the {@code /{profileId}} template, so there's no ambiguity.</p>
     */
    @GetMapping("/feed")
    @PreAuthorize("!hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<FeedItemResponse>> feed(
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(service.listFeed(limit));
    }

    /**
     * Public gallery for a specific photographer.
     * Photographers themselves are blocked (same rule as the browse endpoints).
     */
    @GetMapping("/{profileId}/portfolio")
    @PreAuthorize("!hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<PortfolioItemResponse>> listForProfile(
            @PathVariable UUID profileId,
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(service.listForProfile(profileId, mediaType, category));
    }
}
