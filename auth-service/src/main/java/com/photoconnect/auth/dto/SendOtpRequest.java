package com.photoconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Input for {@code POST /api/v1/auth/otp/send}.
 *
 * <p>Phone must be E.164 + Indian mobile: {@code +91} followed by a 10-digit
 * number starting with 6/7/8/9 (TRAI mobile numbering scheme).</p>
 */
public record SendOtpRequest(
        @NotBlank
        @Pattern(regexp = "^\\+91[6-9]\\d{9}$",
                 message = "must be a valid Indian mobile in E.164 form, e.g. +919876543210")
        String phone
) {}
