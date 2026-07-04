package com.vidyapeet.attempt.dto;

import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.QuestionType;

/** Question as shown to a student while taking a test: no correct answer. */
public record TakeQuestion(
        Long id,
        QuestionType type,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        Integer marks
) {
    public static TakeQuestion from(Question q) {
        return new TakeQuestion(q.getId(), q.getType(), q.getText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getMarks());
    }
}
