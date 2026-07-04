package com.vidyapeet.attempt;

import java.util.List;

/** Aggregate result of grading an attempt. */
public record GradeOutcome(
        double totalScore,
        List<GradedAnswer> answers
) {
}
