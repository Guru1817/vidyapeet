package com.vidyapeet.auth.dto;

import com.vidyapeet.common.Role;
import com.vidyapeet.user.ThemePreference;

/** Safe view of a user (never exposes the password hash). */
public record UserSummary(
        Long id,
        String name,
        String email,
        Role role,
        Long instituteId,
        String instituteSlug,
        ThemePreference themePreference
) {
}
