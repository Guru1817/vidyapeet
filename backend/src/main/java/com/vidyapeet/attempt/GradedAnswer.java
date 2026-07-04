package com.vidyapeet.attempt;

/** Result of grading a single question within an attempt. */
public record GradedAnswer(
        Long questionId,
        String selectedAnswer,
        boolean correct,
        double marksAwarded
) {
}
