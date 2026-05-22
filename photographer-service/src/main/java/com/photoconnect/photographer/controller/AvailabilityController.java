package com.photoconnect.photographer.controller;

import com.photoconnect.photographer.dto.AddAvailabilityRequest;
import com.photoconnect.photographer.dto.AvailabilitySlotResponse;
import com.photoconnect.photographer.security.GatewayPrincipal;
import com.photoconnect.photographer.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Availability calendar — photographer-side editing + customer-side reading.
 *
 * <h2>URL design</h2>
 * <ul>
 *   <li>{@code POST   /me/availability}              — bulk add (photographer)</li>
 *   <li>{@code GET    /me/availability}              — my calendar (photographer)</li>
 *   <li>{@code DELETE /me/availability/{slotId}}     — remove a slot (photographer)</li>
 *   <li>{@code DELETE /me/availability}              — clear my calendar (photographer)</li>
 *   <li>{@code GET    /{profileId}/availability}     — public: a photographer's
 *       available dates (anonymous, customer, also customer-service via Feign)</li>
 * </ul>
 *
 * <p>The public read is gated by {@code !hasRole('PHOTOGRAPHER')} so logged-in
 * photographers can't poke at competitors' calendars — same rule as the rest
 * of the marketplace browse endpoints.</p>
 */
@Tag(name = "availability", description = "Photographer availability calendar")
@RestController
@RequestMapping("/api/v1/photographers")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService service;

    // ── Owner endpoints ───────────────────────────────────────────────────────

    @Operation(
            summary = "List the caller's own availability calendar",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/availability")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> listMine(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.listMine(caller.userId()));
    }

    @Operation(
            summary = "Bulk-add available dates (idempotent). Returns the full updated calendar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dates added"),
            @ApiResponse(responseCode = "400", description = "Empty dates list or bad payload"),
            @ApiResponse(responseCode = "404", description = "Caller has no photographer profile")
    })
    @PostMapping("/me/availability")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> add(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @Valid @RequestBody AddAvailabilityRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.addBulk(caller.userId(), request));
    }

    @Operation(
            summary = "Remove a single availability slot owned by the caller",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me/availability/{slotId}")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID slotId) {
        service.delete(caller.userId(), slotId);
    }

    @Operation(
            summary = "Clear the caller's entire availability calendar",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me/availability")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll(@AuthenticationPrincipal GatewayPrincipal caller) {
        service.clearAll(caller.userId());
    }

    // ── Public read (anonymous, customer, customer-service via Feign) ────────

    @Operation(summary = "Public: list a photographer's available dates")
    @GetMapping("/{profileId}/availability")
    @PreAuthorize("!hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> listForProfile(
            @PathVariable UUID profileId) {
        return ResponseEntity.ok(service.listForProfile(profileId));
    }
}
