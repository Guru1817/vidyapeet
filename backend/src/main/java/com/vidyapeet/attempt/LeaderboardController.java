package com.vidyapeet.attempt;

import com.vidyapeet.attempt.dto.LeaderboardEntry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Per-test leaderboard, visible to the institute's admin and to students enrolled
 * in the test's batch (authorization enforced in the service).
 */
@RestController
@RequestMapping("/api/tests")
public class LeaderboardController {

    private final TakeTestService takeTestService;

    public LeaderboardController(TakeTestService takeTestService) {
        this.takeTestService = takeTestService;
    }

    @GetMapping("/{testId}/leaderboard")
    @PreAuthorize("isAuthenticated()")
    public List<LeaderboardEntry> leaderboard(@PathVariable Long testId) {
        return takeTestService.leaderboard(testId);
    }
}
