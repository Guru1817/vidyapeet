package com.vidyapeet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials. {@code slug} identifies the institute portal for
 * INSTITUTE_ADMIN / STUDENT logins, and is omitted for SUPER_ADMIN.
 */
public record LoginRequest(
        String slug,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
