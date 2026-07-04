package com.vidyapeet.exam;

/**
 * Supported question formats.
 * <ul>
 *   <li>{@code MCQ} – single correct option (A–D).</li>
 *   <li>{@code MSQ} – multiple correct options (A–D); all-or-nothing grading.</li>
 *   <li>{@code TRUE_FALSE} – a statement marked True or False.</li>
 *   <li>{@code FILL_BLANK} – free-text answer matched case-insensitively against
 *       one or more accepted answers.</li>
 * </ul>
 */
public enum QuestionType {
    MCQ,
    MSQ,
    TRUE_FALSE,
    FILL_BLANK
}
