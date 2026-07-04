package com.vidyapeet.institute.dto;

/** Public, data-driven branding for an institute portal. */
public record BrandingResponse(
        String name,
        String slug,
        String logoUrl,
        String primaryColor
) {
}
