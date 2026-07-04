package com.vidyapeet.user.dto;

import com.vidyapeet.user.User;

public record StudentResponse(
        Long id,
        String name,
        String email,
        String description
) {
    public static StudentResponse from(User user) {
        return new StudentResponse(user.getId(), user.getName(), user.getEmail(), user.getDescription());
    }
}
