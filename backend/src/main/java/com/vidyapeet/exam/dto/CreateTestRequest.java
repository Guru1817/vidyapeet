package com.vidyapeet.exam.dto;

import com.vidyapeet.exam.TestType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Creates a test. Provide exactly one of {@code batchId} (a batch-native test)
 * or {@code folderId} (a reusable library test).
 */
public record CreateTestRequest(
        Long batchId,

        Long folderId,

        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        TestType testType,

        boolean negativeMarking,

        @PositiveOrZero(message = "Negative mark per wrong answer cannot be negative")
        Double negativeMarkPerWrong
) {
}
