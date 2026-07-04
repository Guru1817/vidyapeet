package com.vidyapeet.attempt.dto;

import java.time.Instant;
import java.util.List;

public record ResultResponse(
        Long attemptId,
        Long testId,
        String title,
        double score,
        int totalMarks,
        Instant submittedAt,
        List<QuestionResult> breakdown
) {
}
