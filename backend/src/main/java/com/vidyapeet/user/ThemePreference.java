package com.vidyapeet.user;

/**
 * UI colour theme a user has chosen. Persisted on {@code users.theme_preference}
 * and surfaced through {@code UserSummary} / {@code GET /api/auth/me}.
 */
public enum ThemePreference {
    LIGHT,
    DARK
}
