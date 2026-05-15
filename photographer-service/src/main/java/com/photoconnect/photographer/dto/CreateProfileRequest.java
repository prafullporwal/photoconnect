package com.photoconnect.photographer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/photographers/me}.
 *
 * <p>{@code available} is not in this DTO — new profiles start available by
 * default. Photographers can toggle it later via {@link UpdateProfileRequest}.</p>
 */
public record CreateProfileRequest(

        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must be 100 characters or fewer")
        String displayName,

        @Size(max = 2000, message = "Bio must be 2000 characters or fewer")
        String bio,

        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must be 200 characters or fewer")
        String location,

        @NotNull(message = "Years of experience is required")
        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 60, message = "Years of experience cannot exceed 60")
        Integer yearsOfExperience,

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.00", message = "Price per hour cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer and 2 decimal digits")
        BigDecimal pricePerHour,

        @NotNull(message = "Specialties list is required (can be empty)")
        @Size(max = 20, message = "You can list at most 20 specialties")
        List<@NotBlank(message = "Specialty cannot be blank") @Size(max = 100) String> specialties
) {}
