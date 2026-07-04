package com.vidyapeet.attempt.dto;

import jakarta.validation.Valid;

import java.util.List;

public record SubmitAttemptRequest(
        @Valid
        List<AnswerSubmission> answers
) {
}
