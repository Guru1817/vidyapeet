package com.vidyapeet.exam.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for attaching a bank question to a test by reference
 * ({@code POST /api/tests/{id}/references}). The referenced question content is not
 * copied; only a {@code TestQuestionReference} row is created. {@code sectionId} is
 * optional and groups the reference under a test section (null = ungrouped).
 */
public record CreateReferenceRequest(
        @NotNull(message = "bankQuestionId is required")
        Long bankQuestionId,

        Long sectionId
) {
}
