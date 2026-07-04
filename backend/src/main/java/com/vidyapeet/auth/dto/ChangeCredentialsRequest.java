package com.vidyapeet.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Self-service credential update for the authenticated user. The current
 * password is always required to authorize the change. {@code newEmail} and
 * {@code newPassword} are each optional, but at least one must be supplied.
 */
public record ChangeCredentialsRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        String newEmail,

        String newPassword
) {
}
