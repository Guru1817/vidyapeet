package com.vidyapeet.attempt.repository;

import com.vidyapeet.attempt.AttemptStatus;
import com.vidyapeet.attempt.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    Optional<TestAttempt> findByTestIdAndStudentId(Long testId, Long studentId);

    List<TestAttempt> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    /** Leaderboard: submitted attempts for a test, ranked by score. */
    List<TestAttempt> findByTestIdAndStatusOrderByScoreDescSubmittedAtAsc(Long testId, AttemptStatus status);

    Optional<TestAttempt> findFirstByTestIdAndStudentIdAndStatusOrderByStartedAtDesc(
            Long testId, Long studentId, AttemptStatus status);

    Optional<TestAttempt> findFirstByTestIdAndStudentIdAndStatusOrderBySubmittedAtDesc(
            Long testId, Long studentId, AttemptStatus status);

    boolean existsByTestIdAndStudentIdAndStatus(Long testId, Long studentId, AttemptStatus status);

    List<TestAttempt> findByStudentId(Long studentId);

    List<TestAttempt> findByTestId(Long testId);

    void deleteByStudentId(Long studentId);

    void deleteByTestId(Long testId);

    void deleteByInstituteId(Long instituteId);
}
