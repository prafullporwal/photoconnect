package com.photoconnect.photographer.controller;

import com.photoconnect.photographer.dto.CreateProfileRequest;
import com.photoconnect.photographer.dto.PhotographerProfileResponse;
import com.photoconnect.photographer.dto.UpdateProfileRequest;
import com.photoconnect.photographer.security.GatewayPrincipal;
import com.photoconnect.photographer.service.PhotographerService;
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
 * REST controller for photographer profile operations.
 *
 * <h2>URL design</h2>
 * <ul>
 *   <li>{@code /api/v1/photographers}       — public browse (no auth)</li>
 *   <li>{@code /api/v1/photographers/{id}}  — public profile by ID (no auth)</li>
 *   <li>{@code /api/v1/photographers/me}    — authenticated photographer's own profile</li>
 * </ul>
 *
 * <p>Spring MVC resolves {@code /me} as an exact match before treating it as
 * an {@code {id}} template, so there's no ambiguity between the two GET methods.</p>
 *
 * <h2>{@code @AuthenticationPrincipal}</h2>
 * <p>Spring Security injects the principal from {@code SecurityContextHolder}.
 * {@link com.photoconnect.photographer.security.GatewayAuthenticationFilter}
 * puts a {@link GatewayPrincipal} there after reading the gateway headers,
 * so the controller never touches raw HTTP headers.</p>
 *
 * <h2>{@code @PreAuthorize}</h2>
 * <p>Method-security annotations enforce role checks at the AOP boundary
 * (before the method body runs). A user with {@code ROLE_CUSTOMER} hitting
 * {@code POST /me} gets a 403 before any service code executes.</p>
 */
@RestController
@RequestMapping("/api/v1/photographers")
@RequiredArgsConstructor
public class PhotographerController {

    private final PhotographerService service;

    // ── Public endpoints ──────────────────────────────────────────────────────

    /**
     * Browse all photographers currently available for bookings.
     * No authentication required — this is the marketplace discovery page.
     */
    @GetMapping
    public ResponseEntity<List<PhotographerProfileResponse>> listPhotographers() {
        return ResponseEntity.ok(service.listAvailableProfiles());
    }

    /**
     * View a specific photographer's public profile.
     * No authentication required.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PhotographerProfileResponse> getPhotographer(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getProfileById(id));
    }

    // ── Authenticated endpoints (PHOTOGRAPHER role required) ──────────────────

    /** Get the authenticated photographer's own profile. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<PhotographerProfileResponse> getOwnProfile(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.getOwnProfile(caller.userId()));
    }

    /**
     * Create a profile for the authenticated photographer.
     * A user can only create one profile — subsequent attempts return 409.
     */
    @PostMapping("/me")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<PhotographerProfileResponse> createProfile(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createProfile(caller.userId(), request));
    }

    /**
     * Full profile replacement (PUT semantics — all fields required).
     * Photographers can also toggle {@code available} here to pause bookings.
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<PhotographerProfileResponse> updateProfile(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(service.updateProfile(caller.userId(), request));
    }

    /** Permanently delete the photographer's own profile. */
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        service.deleteProfile(caller.userId());
        return ResponseEntity.noContent().build();
    }
}
