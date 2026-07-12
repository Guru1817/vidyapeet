package com.vidyapeet.attempt;

import com.vidyapeet.attempt.dto.AnswerSubmission;
import com.vidyapeet.attempt.dto.LeaderboardEntry;
import com.vidyapeet.attempt.dto.QuestionResult;
import com.vidyapeet.attempt.dto.ResultResponse;
import com.vidyapeet.attempt.dto.StartedTestResponse;
import com.vidyapeet.attempt.dto.StudentTestSummary;
import com.vidyapeet.attempt.dto.SubmitAttemptRequest;
import com.vidyapeet.attempt.dto.TakeQuestion;
import com.vidyapeet.attempt.repository.AttemptAnswerRepository;
import com.vidyapeet.attempt.repository.TestAttemptRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.BatchTest;
import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.TestType;
import com.vidyapeet.exam.dto.SectionResponse;
import com.vidyapeet.exam.repository.BatchTestRepository;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.security.UserPrincipal;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Student test-taking: starting/resuming/re-attempting, auto-grading on submit
 * (with optional negative marking), and producing result breakdowns and the
 * per-test leaderboard.
 */
@Service
public class TakeTestService {

    private final MockTestRepository testRepository;
    private final TestQuestionReferenceRepository referenceRepository;
    private final TestSectionRepository sectionRepository;
    private final TestAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final BatchStudentRepository batchStudentRepository;
    private final BatchTestRepository batchTestRepository;
    private final UserRepository userRepository;
    private final Grader grader;

    public TakeTestService(
            MockTestRepository testRepository,
            TestQuestionReferenceRepository referenceRepository,
            TestSectionRepository sectionRepository,
            TestAttemptRepository attemptRepository,
            AttemptAnswerRepository answerRepository,
            BatchStudentRepository batchStudentRepository,
            BatchTestRepository batchTestRepository,
            UserRepository userRepository,
            Grader grader) {
        this.testRepository = testRepository;
        this.referenceRepository = referenceRepository;
        this.sectionRepository = sectionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.batchStudentRepository = batchStudentRepository;
        this.batchTestRepository = batchTestRepository;
        this.userRepository = userRepository;
        this.grader = grader;
    }

    @Transactional(readOnly = true)
    public List<StudentTestSummary> listStudentTests() {
        Long studentId = SecurityUtils.currentUserId();
        List<Long> batchIds = enrolledBatchIds(studentId);
        if (batchIds.isEmpty()) {
            return List.of();
        }
        // Batch-native tests plus library tests shared to the student's batches.
        java.util.LinkedHashMap<Long, MockTest> byId = new java.util.LinkedHashMap<>();
        for (MockTest t : testRepository.findByBatchIdInAndPublishedTrueOrderByCreatedAtDesc(batchIds)) {
            byId.put(t.getId(), t);
        }
        List<Long> assignedIds = batchTestRepository.findByBatchIdIn(batchIds).stream()
                .map(BatchTest::getTestId).toList();
        if (!assignedIds.isEmpty()) {
            for (MockTest t : testRepository.findByIdInAndPublishedTrueOrderByCreatedAtDesc(assignedIds)) {
                byId.putIfAbsent(t.getId(), t);
            }
        }
        List<MockTest> tests = new ArrayList<>(byId.values());

        Map<Long, List<TestAttempt>> attemptsByTest = attemptRepository.findByStudentId(studentId).stream()
                .collect(Collectors.groupingBy(TestAttempt::getTestId));

        List<StudentTestSummary> summaries = new ArrayList<>(tests.size());
        for (MockTest test : tests) {
            List<TestAttempt> attempts = attemptsByTest.getOrDefault(test.getId(), List.of());
            boolean inProgress = attempts.stream().anyMatch(a -> a.getStatus() == AttemptStatus.IN_PROGRESS);
            List<Double> submittedScores = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                    .map(TestAttempt::getScore)
                    .toList();

            AttemptStatus status = inProgress
                    ? AttemptStatus.IN_PROGRESS
                    : (submittedScores.isEmpty() ? null : AttemptStatus.SUBMITTED);
            Double bestScore = submittedScores.stream().max(Double::compareTo).orElse(null);

            summaries.add(new StudentTestSummary(
                    test.getId(), test.getTitle(), test.getDurationMinutes(),
                    test.getTotalMarks(), referenceRepository.countByTestId(test.getId()),
                    test.getTestType(), test.isNegativeMarking(), status, bestScore));
        }
        return summaries;
    }

    @Transactional
    public StartedTestResponse start(Long testId) {
        Long studentId = SecurityUtils.currentUserId();
        MockTest test = requireTakeableTest(testId, studentId);

        // Resume an in-progress attempt if one exists.
        TestAttempt attempt = attemptRepository
                .findFirstByTestIdAndStudentIdAndStatusOrderByStartedAtDesc(testId, studentId, AttemptStatus.IN_PROGRESS)
                .orElse(null);

        if (attempt == null) {
            // No active attempt. EXAM allows only one ever; PRACTICE allows re-attempts.
            if (test.getTestType() == TestType.EXAM
                    && attemptRepository.existsByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.SUBMITTED)) {
                throw Exceptions.conflict("You have already submitted this exam.");
            }
            attempt = new TestAttempt();
            attempt.setTestId(testId);
            attempt.setStudentId(studentId);
            attempt.setStatus(AttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(Instant.now());
            attempt.setScore(0.0);
            attempt = attemptRepository.save(attempt);
        }

        // Section this bank question is grouped under within the test (null = ungrouped),
        // so the take-test view can group questions by section (Req 7.5).
        Map<Long, Long> sectionByQuestionId = new HashMap<>();
        referenceRepository.findByTestIdOrderBySectionPositionAscPositionAsc(testId)
                .forEach(reference -> sectionByQuestionId.put(
                        reference.getBankQuestionId(), reference.getSectionId()));
        List<TakeQuestion> questions = referenceRepository.findResolvedQuestions(testId).stream()
                .map(q -> TakeQuestion.from(q, sectionByQuestionId.get(q.getId())))
                .toList();
        List<SectionResponse> sections = sectionRepository.findByTestIdOrderByPositionAsc(testId).stream()
                .map(SectionResponse::from)
                .toList();
        // Single overall timer (Req 7.4): deadline is startedAt + durationMinutes, never a
        // per-section limit.
        Instant deadline = attempt.getStartedAt().plus(Duration.ofMinutes(test.getDurationMinutes()));
        return new StartedTestResponse(
                attempt.getId(), test.getId(), test.getTitle(), test.getDurationMinutes(),
                attempt.getStartedAt(), deadline, sections, questions);
    }

    @Transactional
    public ResultResponse submit(Long attemptId, SubmitAttemptRequest request) {
        Long studentId = SecurityUtils.currentUserId();
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> Exceptions.notFound("No attempt found with id " + attemptId + "."));
        if (!attempt.getStudentId().equals(studentId)) {
            throw Exceptions.forbidden("This attempt does not belong to you.");
        }
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw Exceptions.conflict("This attempt has already been submitted.");
        }

        MockTest test = requireTest(attempt.getTestId());
        List<Question> questions = referenceRepository.findResolvedQuestions(attempt.getTestId());

        Map<Long, String> selections = toSelections(request, questions);
        GradeOutcome outcome = grader.grade(
                questions, selections, test.isNegativeMarking(), test.getNegativeMarkPerWrong());

        List<AttemptAnswer> answers = new ArrayList<>(outcome.answers().size());
        for (GradedAnswer graded : outcome.answers()) {
            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttemptId(attempt.getId());
            answer.setQuestionId(graded.questionId());
            answer.setSelectedAnswer(graded.selectedAnswer());
            answer.setCorrect(graded.correct());
            answer.setMarksAwarded(graded.marksAwarded());
            answers.add(answer);
        }
        answerRepository.saveAll(answers);

        attempt.setScore(outcome.totalScore());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attemptRepository.save(attempt);

        return buildResult(attempt, test, questions, gradedByQuestion(outcome));
    }

    @Transactional(readOnly = true)
    public ResultResponse getResultByAttempt(Long attemptId) {
        Long studentId = SecurityUtils.currentUserId();
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> Exceptions.notFound("No attempt found with id " + attemptId + "."));
        if (!attempt.getStudentId().equals(studentId)) {
            throw Exceptions.forbidden("This attempt does not belong to you.");
        }
        return resultForAttempt(attempt);
    }

    @Transactional(readOnly = true)
    public ResultResponse getResultForTest(Long testId) {
        Long studentId = SecurityUtils.currentUserId();
        TestAttempt attempt = attemptRepository
                .findFirstByTestIdAndStudentIdAndStatusOrderBySubmittedAtDesc(testId, studentId, AttemptStatus.SUBMITTED)
                .orElseThrow(() -> Exceptions.notFound("You have not submitted this test yet."));
        return resultForAttempt(attempt);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(Long testId) {
        MockTest test = requireTest(testId);
        authorizeTestView(test);

        List<TestAttempt> attempts = attemptRepository
                .findByTestIdAndStatusOrderByScoreDescSubmittedAtAsc(testId, AttemptStatus.SUBMITTED);
        if (attempts.isEmpty()) {
            return List.of();
        }

        // Keep each student's best attempt (list already ordered by score desc,
        // then earliest submission for ties).
        Map<Long, TestAttempt> bestByStudent = new LinkedHashMap<>();
        for (TestAttempt attempt : attempts) {
            bestByStudent.putIfAbsent(attempt.getStudentId(), attempt);
        }

        List<Long> studentIds = new ArrayList<>(bestByStudent.keySet());
        Map<Long, String> names = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<LeaderboardEntry> entries = new ArrayList<>(bestByStudent.size());
        int rank = 0;
        int processed = 0;
        Double lastScore = null;
        for (TestAttempt attempt : bestByStudent.values()) {
            processed++;
            if (lastScore == null || !attempt.getScore().equals(lastScore)) {
                rank = processed;
                lastScore = attempt.getScore();
            }
            entries.add(new LeaderboardEntry(
                    rank,
                    attempt.getStudentId(),
                    names.getOrDefault(attempt.getStudentId(), "Unknown"),
                    attempt.getScore(),
                    attempt.getSubmittedAt()));
        }
        return entries;
    }

    // --- helpers ---

    private ResultResponse resultForAttempt(TestAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw Exceptions.badRequest("This attempt has not been submitted yet.");
        }
        MockTest test = requireTest(attempt.getTestId());
        List<Question> questions = referenceRepository.findResolvedQuestions(attempt.getTestId());
        Map<Long, AttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity()));

        List<QuestionResult> breakdown = questions.stream().map(q -> {
            AttemptAnswer a = answers.get(q.getId());
            return new QuestionResult(
                    q.getId(), q.getType(), q.getText(),
                    q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                    q.getCorrectAnswer(),
                    a == null ? null : a.getSelectedAnswer(),
                    a != null && a.isCorrect(),
                    a == null ? 0.0 : a.getMarksAwarded(),
                    q.getMarks(), q.getImageKey());
        }).toList();

        return new ResultResponse(attempt.getId(), test.getId(), test.getTitle(),
                attempt.getScore(), test.getTotalMarks(), attempt.getSubmittedAt(), breakdown);
    }

    private ResultResponse buildResult(TestAttempt attempt, MockTest test,
                                       List<Question> questions, Map<Long, GradedAnswer> graded) {
        List<QuestionResult> breakdown = questions.stream().map(q -> {
            GradedAnswer g = graded.get(q.getId());
            return new QuestionResult(
                    q.getId(), q.getType(), q.getText(),
                    q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                    q.getCorrectAnswer(),
                    g == null ? null : g.selectedAnswer(),
                    g != null && g.correct(),
                    g == null ? 0.0 : g.marksAwarded(),
                    q.getMarks(), q.getImageKey());
        }).toList();
        return new ResultResponse(attempt.getId(), test.getId(), test.getTitle(),
                attempt.getScore(), test.getTotalMarks(), attempt.getSubmittedAt(), breakdown);
    }

    private Map<Long, GradedAnswer> gradedByQuestion(GradeOutcome outcome) {
        return outcome.answers().stream()
                .collect(Collectors.toMap(GradedAnswer::questionId, Function.identity()));
    }

    private Map<Long, String> toSelections(SubmitAttemptRequest request, List<Question> questions) {
        Set<Long> validIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        Map<Long, String> selections = new HashMap<>();
        if (request != null && request.answers() != null) {
            for (AnswerSubmission a : request.answers()) {
                if (a.questionId() != null && a.answer() != null && !a.answer().isBlank()
                        && validIds.contains(a.questionId())) {
                    selections.put(a.questionId(), a.answer());
                }
            }
        }
        return selections;
    }

    private MockTest requireTakeableTest(Long testId, Long studentId) {
        MockTest test = requireTest(testId);
        if (!test.isPublished()) {
            throw Exceptions.notFound("No test found with id " + testId + ".");
        }
        if (!studentCanAccess(test, studentId)) {
            throw Exceptions.forbidden("You are not enrolled in a batch this test is assigned to.");
        }
        return test;
    }

    /**
     * A student may access a test if enrolled in its native batch or in any batch
     * the (library) test is shared with.
     */
    private boolean studentCanAccess(MockTest test, Long studentId) {
        if (test.getBatchId() != null
                && batchStudentRepository.existsByBatchIdAndStudentId(test.getBatchId(), studentId)) {
            return true;
        }
        for (BatchTest bt : batchTestRepository.findByTestId(test.getId())) {
            if (batchStudentRepository.existsByBatchIdAndStudentId(bt.getBatchId(), studentId)) {
                return true;
            }
        }
        return false;
    }

    private void authorizeTestView(MockTest test) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == Role.INSTITUTE_ADMIN) {
            return; // tenant filter already scopes to the admin's institute
        }
        if (principal.getRole() == Role.STUDENT && studentCanAccess(test, principal.getUserId())) {
            return;
        }
        throw Exceptions.forbidden("You cannot view this test's leaderboard.");
    }

    private MockTest requireTest(Long testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> Exceptions.notFound("No test found with id " + testId + "."));
    }

    private List<Long> enrolledBatchIds(Long studentId) {
        return batchStudentRepository.findByStudentId(studentId).stream()
                .map(bs -> bs.getBatchId())
                .toList();
    }
}
