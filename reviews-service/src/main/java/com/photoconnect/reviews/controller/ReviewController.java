package com.photoconnect.reviews.controller;

import com.photoconnect.reviews.dto.CreateReviewRequest;
import com.photoconnect.reviews.dto.ReviewResponse;
import com.photoconnect.reviews.dto.ReviewSummaryResponse;
import com.photoconnect.reviews.security.GatewayPrincipal;
import com.photoconnect.reviews.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Reviews REST surface.
 *
 * <ul>
 *   <li>{@code POST   /api/v1/reviews}                          — authenticated CUSTOMER creates a review</li>
 *   <li>{@code GET    /api/v1/reviews/mine}                     — authenticated CUSTOMER lists their reviews</li>
 *   <li>{@code GET    /api/v1/reviews/photographer/{profileId}} — anyone reads a photographer's reviews</li>
 *   <li>{@code GET    /api/v1/reviews/summary/{profileId}}      — anyone reads the aggregate (avg + count)</li>
 * </ul>
 *
 * <p>Public reads are intentional: anonymous marketplace visitors need to see
 * star ratings before deciding to log in. The {@code SecurityConfig} permits
 * {@code GET /api/v1/reviews/photographer/**} and {@code .../summary/**} at
 * the HTTP layer; writes are constrained by {@code @PreAuthorize}.</p>
 */
@Tag(name = "reviews", description = "Customer reviews + ratings")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @Operation(summary = "Create a review (one per completed booking)")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createReview(caller.userId(), request));
    }

    @Operation(summary = "List reviews I have authored")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReviewResponse>> myReviews(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.listMine(caller.userId()));
    }

    @Operation(summary = "List reviews for a photographer (public)")
    @GetMapping("/photographer/{profileId}")
    public ResponseEntity<List<ReviewResponse>> reviewsForPhotographer(
            @PathVariable UUID profileId) {
        return ResponseEntity.ok(service.listForPhotographer(profileId));
    }

    @Operation(summary = "Aggregate rating for a photographer (public)")
    @GetMapping("/summary/{profileId}")
    public ResponseEntity<ReviewSummaryResponse> photographerSummary(
            @PathVariable UUID profileId) {
        return ResponseEntity.ok(service.summarise(profileId));
    }
}
