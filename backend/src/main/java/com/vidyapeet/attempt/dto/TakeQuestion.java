package com.vidyapeet.attempt.dto;

import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.QuestionType;

/**
 * Question as shown to a student while taking a test: no correct answer. {@code sectionId}
 * is the section this question is grouped under within the test ({@code null} = ungrouped),
 * so the take-test view can group questions by section (Req 7.5) or render one ungrouped list
 * when the test defines no sections (Req 7.8).
 */
public record TakeQuestion(
        Long id,
        QuestionType type,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        Integer marks,
        String imageKey,
        Long sectionId
) {
    public static TakeQuestion from(Question q) {
        return from(q, null);
    }

    public static TakeQuestion from(Question q, Long sectionId) {
        return new TakeQuestion(q.getId(), q.getType(), q.getText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getMarks(),
                q.getImageKey(), sectionId);
    }
}
