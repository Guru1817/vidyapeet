package com.vidyapeet.performance;

import com.vidyapeet.attempt.AttemptStatus;
import com.vidyapeet.attempt.TestAttempt;
import com.vidyapeet.attempt.repository.TestAttemptRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.performance.dto.AttemptSummary;
import com.vidyapeet.performance.dto.PerformanceSummary;
import com.vidyapeet.performance.dto.StudentPerformanceRow;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.user.StudentAdminService;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PerformanceService {

    private final TestAttemptRepository attemptRepository;
    private final MockTestRepository testRepository;
    private final UserRepository userRepository;
    private final StudentAdminService studentAdminService;

    public PerformanceService(
            TestAttemptRepository attemptRepository,
            MockTestRepository testRepository,
            UserRepository userRepository,
            StudentAdminService studentAdminService) {
        this.attemptRepository = attemptRepository;
        this.testRepository = testRepository;
        this.userRepository = userRepository;
        this.studentAdminService = studentAdminService;
    }

    /** The logged-in student's own performance. */
    @Transactional(readOnly = true)
    public PerformanceSummary myPerformance() {
        return summaryFor(SecurityUtils.currentUserId());
    }

    /** A specific student's performance (admin); validates the student is in the institute. */
    @Transactional(readOnly = true)
    public PerformanceSummary studentPerformance(Long studentId) {
        studentAdminService.requireStudentInCurrentInstitute(studentId);
        return summaryFor(studentId);
    }

    /** Per-student overview for the institute. */
    @Transactional(readOnly = true)
    public List<StudentPerformanceRow> instituteOverview() {
        Long instituteId = SecurityUtils.currentInstituteId();
        return userRepository.findByInstituteIdAndRole(instituteId, Role.STUDENT).stream()
                .map(student -> {
                    PerformanceSummary s = summaryFor(student.getId());
                    return new StudentPerformanceRow(
                            student.getId(), student.getName(), student.getEmail(),
                            s.totalAttempts(), s.averagePercent());
                })
                .toList();
    }

    private PerformanceSummary summaryFor(Long studentId) {
        List<TestAttempt> submitted = attemptRepository.findByStudentId(studentId).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .toList();
        if (submitted.isEmpty()) {
            return PerformanceSummary.empty();
        }

        List<Long> testIds = submitted.stream().map(TestAttempt::getTestId).distinct().toList();
        Map<Long, MockTest> tests = testRepository.findAllById(testIds).stream()
                .collect(Collectors.toMap(MockTest::getId, Function.identity()));

        List<AttemptSummary> attempts = submitted.stream()
                .sorted(Comparator.comparing(TestAttempt::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(a -> {
                    MockTest t = tests.get(a.getTestId());
                    int total = t == null ? 0 : t.getTotalMarks();
                    double percent = total > 0 ? round1(a.getScore() / total * 100) : 0;
                    return new AttemptSummary(
                            a.getId(), a.getTestId(),
                            t == null ? "(deleted test)" : t.getTitle(),
                            a.getScore(), total, percent, a.getSubmittedAt());
                })
                .toList();

        double avg = round1(attempts.stream().mapToDouble(AttemptSummary::percent).average().orElse(0));
        double best = round1(attempts.stream().mapToDouble(AttemptSummary::percent).max().orElse(0));
        long testsAttempted = testIds.size();

        return new PerformanceSummary(testsAttempted, attempts.size(), avg, best, attempts);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
