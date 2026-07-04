package com.vidyapeet.institute.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Creates a coaching center plus its first INSTITUTE_ADMIN account, so the portal
 * is immediately usable.
 */
public record CreateInstituteRequest(
        @NotBlank(message = "Institute name is required")
        String name,

        @NotBlank(message = "Slug is required")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase letters, numbers and hyphens (e.g. 'demo-classes')")
        String slug,

        String logoUrl,

        @Pattern(
                regexp = "^#([0-9a-fA-F]{6})$",
                message = "Primary color must be a hex value like #2563EB")
        String primaryColor,

        @NotBlank(message = "Admin name is required")
        String adminName,

        @NotBlank(message = "Admin email is required")
        @Email(message = "A valid admin email is required")
        String adminEmail,

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, message = "Admin password must be at least 8 characters")
        String adminPassword
) {
}
