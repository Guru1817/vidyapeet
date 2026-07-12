package com.vidyapeet.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PUT /api/auth/me/theme}. The raw string is validated
 * against the {@code ThemePreference} enum in the service layer so an invalid
 * value surfaces as a clean {@code 400} rather than a deserialization failure.
 */
public record ThemeUpdateRequest(
        @NotBlank(message = "Theme is required")
        String theme
) {
}
