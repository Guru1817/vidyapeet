package com.vidyapeet.attempt.dto;

import java.time.Instant;
import java.util.List;

/**
 * Payload returned when a student starts (or resumes) a test. {@code deadline}
 * lets the client run a countdown and auto-submit on timeout.
 */
public record StartedTestResponse(
        Long attemptId,
        Long testId,
        String title,
        Integer durationMinutes,
        Instant startedAt,
        Instant deadline,
        List<TakeQuestion> questions
) {
}
