package com.vidyapeet.attempt;

import com.vidyapeet.exam.AnswerOption;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for the auto-grading logic across all question types. No Spring
 * context required.
 */
class GraderTest {

    private final Grader grader = new Grader();

    private Question q(long id, QuestionType type, String correctAnswer, int marks) {
        Question question = new Question();
        question.setId(id);
        question.setType(type);
        question.setText("Q" + id);
        question.setCorrectAnswer(correctAnswer);
        question.setMarks(marks);
        return question;
    }

    @Test
    void gradesMcqMsqTrueFalseAndFillBlank() {
        List<Question> questions = List.of(
                q(1, QuestionType.MCQ, "A", 1),
                q(2, QuestionType.MSQ, "A,C", 2),
                q(3, QuestionType.TRUE_FALSE, "TRUE", 1),
                q(4, QuestionType.FILL_BLANK, "newton|newtons", 2));

        Map<Long, String> answers = Map.of(
                1L, "A",        // correct
                2L, "C,A",      // correct (order-insensitive set)
                3L, "true",     // correct (case-insensitive)
                4L, "  Newton "); // correct (trim + case-insensitive)

        GradeOutcome outcome = grader.grade(questions, answers, false, 0);

        assertThat(outcome.totalScore()).isEqualTo(6.0);
        assertThat(outcome.answers()).allMatch(GradedAnswer::correct);
    }

    @Test
    void msqRequiresExactSet() {
        List<Question> questions = List.of(q(1, QuestionType.MSQ, "A,C", 3));
        // Missing C -> incorrect (all-or-nothing).
        GradeOutcome outcome = grader.grade(questions, Map.of(1L, "A"), false, 0);
        assertThat(outcome.totalScore()).isZero();
        assertThat(outcome.answers().get(0).correct()).isFalse();
    }

    @Test
    void fillBlankWrongTextScoresZero() {
        List<Question> questions = List.of(q(1, QuestionType.FILL_BLANK, "newton", 2));
        GradeOutcome outcome = grader.grade(questions, Map.of(1L, "joule"), false, 0);
        assertThat(outcome.totalScore()).isZero();
    }

    @Test
    void wrongAnswerIsPenalisedWhenNegativeMarkingEnabled() {
        List<Question> questions = List.of(
                q(1, QuestionType.MCQ, "A", 2),
                q(2, QuestionType.MCQ, "C", 3));
        Map<Long, String> answers = Map.of(1L, "A", 2L, "D");

        GradeOutcome outcome = grader.grade(questions, answers, true, 0.25);

        assertThat(outcome.totalScore()).isCloseTo(1.75, within(1e-9));
        assertThat(outcome.answers().get(1).marksAwarded()).isCloseTo(-0.25, within(1e-9));
    }

    @Test
    void unansweredQuestionIsNeverPenalised() {
        List<Question> questions = List.of(
                q(1, QuestionType.MCQ, "A", 2),
                q(2, QuestionType.MCQ, "C", 3));
        GradeOutcome outcome = grader.grade(questions, Map.of(1L, "A"), true, 1.0);

        assertThat(outcome.totalScore()).isEqualTo(2.0);
        GradedAnswer second = outcome.answers().get(1);
        assertThat(second.selectedAnswer()).isNull();
        assertThat(second.marksAwarded()).isZero();
    }

    @Test
    void emptySubmissionScoresZero() {
        List<Question> questions = List.of(
                q(1, QuestionType.MCQ, "A", 2),
                q(2, QuestionType.MSQ, "A,B", 3));

        GradeOutcome outcome = grader.grade(questions, Map.of(), false, 0);

        assertThat(outcome.totalScore()).isZero();
        assertThat(outcome.answers()).hasSize(2);
        assertThat(outcome.answers()).noneMatch(GradedAnswer::correct);
    }

    // Keeps AnswerOption referenced for clarity of the option-based types.
    @Test
    void mcqOptionEnumNamesAreCanonical() {
        assertThat(AnswerOption.A.name()).isEqualTo("A");
    }
}
