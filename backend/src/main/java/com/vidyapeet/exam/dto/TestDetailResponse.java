package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.TestType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Admin test detail including its full question list (with answers) and the test's
 * labeled sections. Each question carries the {@code sectionId} it is grouped under within
 * this test ({@code null} = ungrouped) so the editor can render questions grouped by section
 * (Req 7.5) and fall back to a single ungrouped list when the test defines no sections
 * (Req 7.8). Questions are already ordered by section position then reference position.
 */
public record TestDetailResponse(
        Long id,
        Long batchId,
        Long folderId,
        String title,
        Integer durationMinutes,
        Integer totalMarks,
        boolean published,
        TestType testType,
        boolean negativeMarking,
        double negativeMarkPerWrong,
        Instant createdAt,
        List<SectionResponse> sections,
        List<QuestionResponse> questions
) {
    public static TestDetailResponse from(
            MockTest test,
            List<Question> questions,
            Map<Long, Long> sectionByQuestionId,
            List<SectionResponse> sections) {
        return new TestDetailResponse(
                test.getId(),
                test.getBatchId(),
                test.getFolderId(),
                test.getTitle(),
                test.getDurationMinutes(),
                test.getTotalMarks(),
                test.isPublished(),
                test.getTestType(),
                test.isNegativeMarking(),
                test.getNegativeMarkPerWrong(),
                test.getCreatedAt(),
                sections,
                questions.stream()
                        .map(q -> QuestionResponse.from(q, sectionByQuestionId.get(q.getId())))
                        .toList());
    }
}
