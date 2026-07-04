package com.vidyapeet.performance.dto;

import java.util.List;

/**
 * A student's performance across all submitted attempts.
 */
public record PerformanceSummary(
        long testsAttempted,
        int totalAttempts,
        double averagePercent,
        double bestPercent,
        List<AttemptSummary> attempts
) {
    public static PerformanceSummary empty() {
        return new PerformanceSummary(0, 0, 0, 0, List.of());
    }
}
