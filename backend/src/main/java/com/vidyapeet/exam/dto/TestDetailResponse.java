package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.TestType;

import java.time.Instant;
import java.util.List;

/** Admin test detail including its full question list (with answers). */
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
        List<QuestionResponse> questions
) {
    public static TestDetailResponse from(MockTest test, List<Question> questions) {
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
                questions.stream().map(QuestionResponse::from).toList());
    }
}
