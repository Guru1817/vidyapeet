package com.vidyapeet.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Updates a student's profile. Password is optional — provide it only to reset
 * the student's password.
 */
public record UpdateStudentRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        String email,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
