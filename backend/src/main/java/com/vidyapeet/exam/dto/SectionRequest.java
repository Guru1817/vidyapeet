package com.vidyapeet.exam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload to create, rename, or reorder a {@code TestSection}
 * ({@code POST/PUT /api/tests/{id}/sections}). {@code label} is the section title;
 * {@code position} is its zero-based order within the test. When {@code position} is
 * omitted on create the section is appended at the end; on update a null {@code position}
 * leaves the existing order unchanged.
 */
public record SectionRequest(
        @NotBlank(message = "label is required")
        String label,

        Integer position
) {
}
