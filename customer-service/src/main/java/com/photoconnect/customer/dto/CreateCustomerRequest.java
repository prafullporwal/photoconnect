package com.photoconnect.customer.dto;

import com.photoconnect.customer.domain.ContactMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for {@code POST /api/v1/customers/me}. */
public record CreateCustomerRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 200) String location,
        @Size(max = 30)  String phoneNumber,
        @NotNull         ContactMethod preferredContactMethod
) {}
