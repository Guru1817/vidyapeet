package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.AnswerOption;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.QuestionType;

import java.util.Arrays;
import java.util.List;

/**
 * Admin-facing question view. Includes the canonical correct answer plus the
 * decoded structured fields so the editor can prefill the right inputs.
 */
public record QuestionResponse(
        Long id,
        QuestionType type,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctAnswer,
        AnswerOption correctOption,
        List<AnswerOption> correctOptions,
        Boolean correctBoolean,
        List<String> acceptedAnswers,
        Integer marks
) {
    public static QuestionResponse from(Question q) {
        AnswerOption correctOption = null;
        List<AnswerOption> correctOptions = null;
        Boolean correctBoolean = null;
        List<String> acceptedAnswers = null;
        String ca = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer();

        switch (q.getType()) {
            case MCQ -> correctOption = parseOption(ca);
            case MSQ -> correctOptions = Arrays.stream(ca.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(AnswerOption::valueOf).toList();
            case TRUE_FALSE -> correctBoolean = "TRUE".equalsIgnoreCase(ca.trim());
            case FILL_BLANK -> acceptedAnswers = Arrays.stream(ca.split("\\|"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
        }

        return new QuestionResponse(
                q.getId(), q.getType(), q.getText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectAnswer(), correctOption, correctOptions, correctBoolean, acceptedAnswers,
                q.getMarks());
    }

    private static AnswerOption parseOption(String s) {
        try {
            return AnswerOption.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
