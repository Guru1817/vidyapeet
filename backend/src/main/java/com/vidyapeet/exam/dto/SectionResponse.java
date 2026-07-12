package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.TestSection;

/**
 * A test section as returned to admins and students so the frontend can group questions
 * under their labels (Req 7.5). Sections carry no timing of their own; the attempt runs
 * under the single overall timer.
 */
public record SectionResponse(
        Long id,
        Long testId,
        String label,
        Integer position
) {
    public static SectionResponse from(TestSection section) {
        return new SectionResponse(
                section.getId(),
                section.getTestId(),
                section.getLabel(),
                section.getPosition());
    }
}
