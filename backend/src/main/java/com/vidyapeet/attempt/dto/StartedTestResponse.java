package com.vidyapeet.attempt.dto;

import com.vidyapeet.exam.dto.SectionResponse;

import java.time.Instant;
import java.util.List;

/**
 * Payload returned when a student starts (or resumes) a test. {@code deadline}
 * lets the client run a countdown and auto-submit on timeout; it is derived solely from
 * {@code startedAt + durationMinutes} — the single overall timer — regardless of sections
 * (Req 7.4, 7.6). {@code sections} lists the test's labeled sections in display order so the
 * take-test view can group questions under them (Req 7.5); it is empty when the test defines
 * no sections, in which case questions render as one ungrouped list (Req 7.8). Each question
 * carries its own {@code sectionId} for grouping.
 */
public record StartedTestResponse(
        Long attemptId,
        Long testId,
        String title,
        Integer durationMinutes,
        Instant startedAt,
        Instant deadline,
        List<SectionResponse> sections,
        List<TakeQuestion> questions
) {
}
