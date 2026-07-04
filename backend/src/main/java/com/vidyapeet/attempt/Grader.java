package com.vidyapeet.attempt;

import com.vidyapeet.exam.AnswerCodec;
import com.vidyapeet.exam.Question;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure auto-grading logic for all question types. Has no dependencies so it can
 * be unit-tested in isolation.
 *
 * <ul>
 *   <li>A correct answer awards the question's full marks.</li>
 *   <li>An unanswered question scores zero (never penalised).</li>
 *   <li>A wrong, answered question scores zero, or {@code -negativeMarkPerWrong}
 *       when negative marking is enabled.</li>
 *   <li>MSQ grading is all-or-nothing (the selected set must match exactly).</li>
 * </ul>
 */
@Component
public class Grader {

    public GradeOutcome grade(
            List<Question> questions,
            Map<Long, String> answersByQuestionId,
            boolean negativeMarking,
            double negativeMarkPerWrong) {

        List<GradedAnswer> graded = new ArrayList<>(questions.size());
        double total = 0;
        for (Question question : questions) {
            String selected = answersByQuestionId.get(question.getId());
            boolean answered = selected != null && !selected.isBlank();
            boolean correct = answered
                    && AnswerCodec.isCorrect(question.getType(), question.getCorrectAnswer(), selected);

            double awarded;
            if (correct) {
                awarded = question.getMarks();
            } else if (answered && negativeMarking) {
                awarded = -Math.abs(negativeMarkPerWrong);
            } else {
                awarded = 0;
            }

            total += awarded;
            graded.add(new GradedAnswer(question.getId(), answered ? selected : null, correct, awarded));
        }
        return new GradeOutcome(total, graded);
    }
}
