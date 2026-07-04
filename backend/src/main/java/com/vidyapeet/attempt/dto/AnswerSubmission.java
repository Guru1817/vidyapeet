package com.vidyapeet.attempt.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A single answer in a submission. {@code answer} is the canonical string for the
 * question's type (e.g. "B" for MCQ, "A,C" for MSQ, "TRUE"/"FALSE", or free text
 * for fill-in-the-blank). Null/blank means the question was skipped.
 */
public record AnswerSubmission(
        @NotNull(message = "questionId is required")
        Long questionId,

        String answer
) {
}
