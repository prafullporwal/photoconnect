package com.photoconnect.customer.controller;

import com.photoconnect.customer.dto.FavoriteResponse;
import com.photoconnect.customer.dto.FavoriteStatusResponse;
import com.photoconnect.customer.security.GatewayPrincipal;
import com.photoconnect.customer.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Favorites endpoints — "save content" bookmarks on individual portfolio items.
 *
 * <ul>
 *   <li>{@code PUT    /api/v1/favorites/{portfolioItemId}}        — save (idempotent)</li>
 *   <li>{@code DELETE /api/v1/favorites/{portfolioItemId}}        — unsave (idempotent)</li>
 *   <li>{@code GET    /api/v1/favorites}                          — my saved content (enriched)</li>
 *   <li>{@code GET    /api/v1/favorites/{portfolioItemId}/status} — is this saved? (heart state)</li>
 * </ul>
 */
@Tag(name = "favorites", description = "Customer bookmarks (\"save content\")")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService service;

    @Operation(
            summary = "Save a portfolio item to the caller's favorites (idempotent)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved (or already saved)"),
            @ApiResponse(responseCode = "400", description = "Portfolio item not found"),
            @ApiResponse(responseCode = "503", description = "photographer-service unavailable")
    })
    @PutMapping("/{portfolioItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteResponse> save(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID portfolioItemId) {
        return ResponseEntity.ok(service.save(caller.userId(), portfolioItemId));
    }

    @Operation(
            summary = "Remove a saved item (idempotent — no error if it wasn't saved)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Unsaved")
    @DeleteMapping("/{portfolioItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID portfolioItemId) {
        service.remove(caller.userId(), portfolioItemId);
    }

    @Operation(
            summary = "List the caller's saved content, enriched with media + photographer info",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<FavoriteResponse>> list(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.list(caller.userId()));
    }

    @Operation(
            summary = "Is this portfolio item in the caller's favorites? (drives heart icon state)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{portfolioItemId}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteStatusResponse> status(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID portfolioItemId) {
        return ResponseEntity.ok(service.getStatus(caller.userId(), portfolioItemId));
    }
}
