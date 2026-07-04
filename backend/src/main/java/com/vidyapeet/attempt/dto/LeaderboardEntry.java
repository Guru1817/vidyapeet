package com.vidyapeet.attempt.dto;

import java.time.Instant;

public record LeaderboardEntry(
        int rank,
        Long studentId,
        String studentName,
        double score,
        Instant submittedAt
) {
}
