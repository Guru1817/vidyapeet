package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.TestType;

import java.time.Instant;

public record TestResponse(
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
        long questionCount,
        Instant createdAt
) {
    public static TestResponse from(MockTest test, long questionCount) {
        return new TestResponse(
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
                questionCount,
                test.getCreatedAt());
    }
}
