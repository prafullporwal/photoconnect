package com.photoconnect.customer.controller;

import com.photoconnect.customer.dto.CreateInquiryRequest;
import com.photoconnect.customer.dto.InquiryResponse;
import com.photoconnect.customer.dto.UpdateInquiryStatusRequest;
import com.photoconnect.customer.security.GatewayPrincipal;
import com.photoconnect.customer.service.InquiryService;
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
 * Inquiry endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/v1/inquiries}          — customer creates a new inquiry</li>
 *   <li>{@code GET  /api/v1/inquiries/mine}     — customer's outbox</li>
 *   <li>{@code GET  /api/v1/inquiries/received} — photographer's inbox</li>
 *   <li>{@code GET  /api/v1/inquiries/{id}}     — either participant</li>
 *   <li>{@code PATCH /api/v1/inquiries/{id}/status} — either participant updates state</li>
 * </ul>
 *
 * <p>Role enforcement: {@code @PreAuthorize} guards write/list endpoints at
 * the controller boundary. Participant checks for individual inquiries happen
 * in {@link InquiryService} (it needs to load the row to know who's involved).</p>
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService service;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<InquiryResponse> createInquiry(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @Valid @RequestBody CreateInquiryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createInquiry(caller.userId(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<InquiryResponse>> myInquiries(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.listMyInquiries(caller.userId()));
    }

    @GetMapping("/received")
    @PreAuthorize("hasRole('PHOTOGRAPHER')")
    public ResponseEntity<List<InquiryResponse>> receivedInquiries(
            @AuthenticationPrincipal GatewayPrincipal caller) {
        return ResponseEntity.ok(service.listReceivedInquiries(caller.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InquiryResponse> getInquiry(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getInquiry(id, caller.userId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InquiryResponse> updateStatus(
            @AuthenticationPrincipal GatewayPrincipal caller,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInquiryStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, caller.userId(), request.status()));
    }
}
