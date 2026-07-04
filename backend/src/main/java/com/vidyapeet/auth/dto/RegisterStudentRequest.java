package com.vidyapeet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration for a student into a specific institute portal. The
 * role is always forced to STUDENT; an INSTITUTE_ADMIN must still enroll the
 * student into batches before they can access materials.
 */
public record RegisterStudentRequest(
        @NotBlank(message = "Institute slug is required")
        String slug,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
