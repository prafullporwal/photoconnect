package com.photoconnect.auth.dto;

import com.photoconnect.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input for {@code POST /api/v1/auth/otp/verify}.
 *
 * <p>{@code role} is only consulted when the verify creates a NEW user — for
 * an existing phone we ignore the field and return the user's stored role.
 * {@code email} is optional and only set on first signup.</p>
 */
public record VerifyOtpRequest(
        @NotBlank
        @Pattern(regexp = "^\\+91[6-9]\\d{9}$") String phone,
        @NotBlank @Size(min = 4, max = 10) String code,
        @NotNull Role role,
        @Email @Size(max = 255) String email
) {}
