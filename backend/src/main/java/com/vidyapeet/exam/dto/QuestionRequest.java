package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.AnswerOption;
import com.vidyapeet.exam.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Create/update payload for a question of any type. Only the fields relevant to
 * {@link #type()} are required; the service validates and encodes accordingly.
 */
public record QuestionRequest(
        @NotNull(message = "Question type is required")
        QuestionType type,

        @NotBlank(message = "Question text is required")
        String text,

        // MCQ / MSQ
        String optionA,
        String optionB,
        String optionC,
        String optionD,

        // MCQ
        AnswerOption correctOption,

        // MSQ
        List<AnswerOption> correctOptions,

        // TRUE_FALSE
        Boolean correctBoolean,

        // FILL_BLANK
        List<String> acceptedAnswers,

        @NotNull(message = "Marks are required")
        @Min(value = 1, message = "Marks must be at least 1")
        Integer marks
) {
}
