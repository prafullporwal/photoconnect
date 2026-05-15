package com.photoconnect.customer.dto;

import com.photoconnect.customer.domain.InquiryStatus;
import jakarta.validation.constraints.NotNull;

/** Body for {@code PATCH /api/v1/inquiries/{id}/status}. */
public record UpdateInquiryStatusRequest(
        @NotNull InquiryStatus status
) {}
