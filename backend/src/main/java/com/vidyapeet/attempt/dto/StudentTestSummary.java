package com.vidyapeet.attempt.dto;

import com.vidyapeet.attempt.AttemptStatus;
import com.vidyapeet.exam.TestType;

/**
 * A published test as seen in a student's test list, annotated with the
 * student's own attempt state. For PRACTICE tests the score is the best so far.
 * {@code attemptStatus} is null when never attempted.
 */
public record StudentTestSummary(
        Long testId,
        String title,
        Integer durationMinutes,
        Integer totalMarks,
        long questionCount,
        TestType testType,
        boolean negativeMarking,
        AttemptStatus attemptStatus,
        Double score
) {
}
