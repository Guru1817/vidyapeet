package com.vidyapeet.performance.dto;

/** One row in the institute's per-student performance overview. */
public record StudentPerformanceRow(
        Long studentId,
        String name,
        String email,
        int totalAttempts,
        double averagePercent
) {
}
