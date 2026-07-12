package com.vidyapeet.attempt.dto;

import com.vidyapeet.exam.QuestionType;

/** Per-question breakdown shown on the result screen (after submission). */
public record QuestionResult(
        Long questionId,
        QuestionType type,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctAnswer,
        String selectedAnswer,
        boolean correct,
        double marksAwarded,
        int marks,
        String imageKey
) {
}
