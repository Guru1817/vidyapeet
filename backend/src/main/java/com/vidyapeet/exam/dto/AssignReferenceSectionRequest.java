package com.vidyapeet.exam.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload to group (or ungroup) a bank question's reference within a test under a section
 * ({@code PUT /api/tests/{id}/references/section}). Sets {@code TestQuestionReference.section_id}
 * to the given {@code sectionId}; a null {@code sectionId} ungroups the reference (moves it
 * back to the ungrouped list).
 */
public record AssignReferenceSectionRequest(
        @NotNull(message = "bankQuestionId is required")
        Long bankQuestionId,

        Long sectionId
) {
}
