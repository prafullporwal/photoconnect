package com.photoconnect.auth.dto;

import com.photoconnect.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Input for {@code POST /api/v1/auth/register}.
 *
 * <p>Bean Validation runs on the controller via {@code @Valid} — these
 * annotations produce 400-with-field-errors on bad input, handled by
 * {@code GlobalExceptionHandler}.</p>
 */
public record RegisterRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull Role role
) {}
