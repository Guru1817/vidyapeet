package com.vidyapeet.batch.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollStudentRequest(
        @NotNull(message = "studentId is required")
        Long studentId
) {
}
