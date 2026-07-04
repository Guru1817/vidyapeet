package com.vidyapeet.performance.dto;

import java.time.Instant;

public record AttemptSummary(
        Long attemptId,
        Long testId,
        String testTitle,
        double score,
        int totalMarks,
        double percent,
        Instant submittedAt
) {
}
