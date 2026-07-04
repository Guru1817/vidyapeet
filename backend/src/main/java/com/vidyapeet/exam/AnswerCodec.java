package com.vidyapeet.exam;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Encodes and compares answers in a single canonical string form per question
 * type, so storage and grading share one source of truth.
 *
 * <ul>
 *   <li>MCQ: {@code "B"}</li>
 *   <li>MSQ: sorted, comma-joined letters, e.g. {@code "A,C"}</li>
 *   <li>TRUE_FALSE: {@code "TRUE"} or {@code "FALSE"}</li>
 *   <li>FILL_BLANK: pipe-joined accepted answers, e.g. {@code "newton|newtons"}</li>
 * </ul>
 */
public final class AnswerCodec {

    private AnswerCodec() {
    }

    /** Canonical encoding for an MSQ correct/selected set. */
    public static String encodeOptions(Collection<AnswerOption> options) {
        Set<String> sorted = new TreeSet<>();
        for (AnswerOption o : options) {
            sorted.add(o.name());
        }
        return String.join(",", sorted);
    }

    /** Canonical encoding for fill-in-the-blank accepted answers. */
    public static String encodeAccepted(Collection<String> answers) {
        return answers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("|"));
    }

    /** Whether a student's raw answer is correct for the given question. */
    public static boolean isCorrect(QuestionType type, String correctAnswer, String selected) {
        if (selected == null || selected.isBlank() || correctAnswer == null) {
            return false;
        }
        return switch (type) {
            case MCQ, TRUE_FALSE -> selected.trim().equalsIgnoreCase(correctAnswer.trim());
            case MSQ -> letterSet(selected).equals(letterSet(correctAnswer));
            case FILL_BLANK -> Arrays.stream(correctAnswer.split("\\|"))
                    .map(s -> s.trim().toLowerCase())
                    .anyMatch(a -> a.equals(selected.trim().toLowerCase()));
        };
    }

    private static Set<String> letterSet(String csv) {
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
