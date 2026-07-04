package com.vidyapeet.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        UserSummary user
) {
    public static AuthResponse bearer(String token, UserSummary user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
