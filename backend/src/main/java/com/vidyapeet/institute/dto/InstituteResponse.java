package com.vidyapeet.institute.dto;

import com.vidyapeet.institute.Institute;

import java.time.Instant;

public record InstituteResponse(
        Long id,
        String name,
        String slug,
        String logoUrl,
        String primaryColor,
        Instant createdAt
) {
    public static InstituteResponse from(Institute institute) {
        return new InstituteResponse(
                institute.getId(),
                institute.getName(),
                institute.getSlug(),
                institute.getLogoUrl(),
                institute.getPrimaryColor(),
                institute.getCreatedAt());
    }
}
