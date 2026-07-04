package com.vidyapeet.institute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Updates a center's display name and branding. The slug is intentionally
 * immutable so existing portal URLs and student logins keep working.
 */
public record UpdateInstituteRequest(
        @NotBlank(message = "Institute name is required")
        String name,

        String logoUrl,

        @Pattern(
                regexp = "^#([0-9a-fA-F]{6})$",
                message = "Primary color must be a hex value like #2563EB")
        String primaryColor
) {
}
