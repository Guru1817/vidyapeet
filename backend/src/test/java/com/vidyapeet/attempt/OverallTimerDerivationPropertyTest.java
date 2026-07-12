package com.vidyapeet.attempt;

import com.vidyapeet.attempt.dto.StartedTestResponse;
import com.vidyapeet.attempt.repository.AttemptAnswerRepository;
import com.vidyapeet.attempt.repository.TestAttemptRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.TestQuestionReference;
import com.vidyapeet.exam.TestSection;
import com.vidyapeet.exam.repository.BatchTestRepository;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.user.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 11: Overall timer derivation is independent of sections
 *
 * <p>For any test and any section configuration, the attempt deadline equals
 * {@code startedAt + durationMinutes} and no per-section time limit is applied.
 *
 * <p>Validates: Requirements 7.4
 *
 * <p>The test exercises the real {@link TakeTestService#start} path against Mockito-backed
 * in-memory repositories (matching the established backend property-test style — no Spring
 * context, no database). {@link SecurityUtils#currentUserId()} is mocked statically to supply
 * the current student. A pre-existing IN_PROGRESS attempt with a known {@code startedAt} is
 * resumed so the deadline is fully deterministic. For each generated {@code durationMinutes}
 * and each section configuration (0..N sections and references grouped/ungrouped in any order),
 * we assert:
 * <ol>
 *   <li>the returned {@code deadline} equals {@code startedAt + durationMinutes} exactly; and</li>
 *   <li>running {@code start} again with <em>no</em> sections (same started-at and duration)
 *       yields the identical deadline — i.e. sections never shorten or extend the timer, and no
 *       per-section limit is applied.</li>
 * </ol>
 */
class OverallTimerDerivationPropertyTest {

    private static final long TEST_ID = 1L;
    private static final long STUDENT_ID = 42L;
    private static final long BATCH_ID = 7L;

    @Property(tries = 100)
    void deadlineIsStartedAtPlusDurationRegardlessOfSections(@ForAll("scenarios") TimerScenario scenario) {
        Instant startedAt = scenario.startedAt();
        int durationMinutes = scenario.durationMinutes();
        Instant expectedDeadline = startedAt.plus(Duration.ofMinutes(durationMinutes));

        // (1) Deadline derived while the test carries the generated section configuration.
        StartedTestResponse withSections = runStart(
                durationMinutes, startedAt, scenario.sections(), scenario.references());

        assertThat(withSections.deadline())
                .as("deadline must equal startedAt + durationMinutes for section config %s", scenario)
                .isEqualTo(expectedDeadline);
        assertThat(withSections.durationMinutes())
                .as("the single overall timer duration is the test duration, untouched by sections")
                .isEqualTo(durationMinutes);
        // Duration between start and deadline is exactly the test duration — no per-section time.
        assertThat(Duration.between(withSections.startedAt(), withSections.deadline()))
                .isEqualTo(Duration.ofMinutes(durationMinutes));

        // (2) Deadline derived for the SAME started-at/duration but with NO sections at all.
        StartedTestResponse withoutSections = runStart(
                durationMinutes, startedAt, List.of(), List.of());

        assertThat(withoutSections.deadline())
                .as("removing all sections must not change the overall-timer deadline")
                .isEqualTo(expectedDeadline)
                .isEqualTo(withSections.deadline());
    }

    /**
     * Builds a fully mocked {@link TakeTestService}, resumes a pre-existing IN_PROGRESS attempt
     * with the given {@code startedAt}, and returns the {@link StartedTestResponse} from
     * {@link TakeTestService#start}.
     */
    private StartedTestResponse runStart(int durationMinutes, Instant startedAt,
                                         List<TestSection> sections, List<TestQuestionReference> references) {
        MockTest test = new MockTest();
        test.setId(TEST_ID);
        test.setTitle("Timed Test");
        test.setDurationMinutes(durationMinutes);
        test.setPublished(true);
        test.setBatchId(BATCH_ID);

        TestAttempt attempt = new TestAttempt();
        attempt.setId(99L);
        attempt.setTestId(TEST_ID);
        attempt.setStudentId(STUDENT_ID);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(startedAt);
        attempt.setScore(0.0);

        MockTestRepository testRepository = mock(MockTestRepository.class);
        when(testRepository.findById(TEST_ID)).thenReturn(Optional.of(test));

        BatchStudentRepository batchStudentRepository = mock(BatchStudentRepository.class);
        when(batchStudentRepository.existsByBatchIdAndStudentId(BATCH_ID, STUDENT_ID)).thenReturn(true);

        BatchTestRepository batchTestRepository = mock(BatchTestRepository.class);

        TestAttemptRepository attemptRepository = mock(TestAttemptRepository.class);
        when(attemptRepository.findFirstByTestIdAndStudentIdAndStatusOrderByStartedAtDesc(
                TEST_ID, STUDENT_ID, AttemptStatus.IN_PROGRESS)).thenReturn(Optional.of(attempt));
        when(attemptRepository.save(any(TestAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TestQuestionReferenceRepository referenceRepository = mock(TestQuestionReferenceRepository.class);
        // Ordered by (section, position) exactly as the JPQL query does against the database.
        List<TestQuestionReference> ordered = references.stream()
                .sorted(Comparator
                        .comparing((TestQuestionReference r) ->
                                r.getSectionId() == null ? Long.MIN_VALUE : r.getSectionId())
                        .thenComparing(TestQuestionReference::getPosition))
                .toList();
        when(referenceRepository.findByTestIdOrderBySectionPositionAscPositionAsc(TEST_ID))
                .thenReturn(ordered);
        // Resolved questions are irrelevant to the timer; return a matching (possibly empty) list.
        List<Question> resolved = new ArrayList<>();
        for (TestQuestionReference ref : ordered) {
            Question q = new Question();
            q.setId(ref.getBankQuestionId());
            resolved.add(q);
        }
        when(referenceRepository.findResolvedQuestions(TEST_ID)).thenReturn(resolved);

        TestSectionRepository sectionRepository = mock(TestSectionRepository.class);
        List<TestSection> orderedSections = sections.stream()
                .sorted(Comparator.comparing(TestSection::getPosition))
                .toList();
        when(sectionRepository.findByTestIdOrderByPositionAsc(TEST_ID)).thenReturn(orderedSections);

        AttemptAnswerRepository answerRepository = mock(AttemptAnswerRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Grader grader = new Grader();

        TakeTestService service = new TakeTestService(
                testRepository, referenceRepository, sectionRepository, attemptRepository,
                answerRepository, batchStudentRepository, batchTestRepository, userRepository, grader);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::currentUserId).thenReturn(STUDENT_ID);
            return service.start(TEST_ID);
        }
    }

    // -------------------------------------------------------------------------
    // Generators
    // -------------------------------------------------------------------------

    /** A test duration, a fixed attempt start instant, and an arbitrary section configuration. */
    record TimerScenario(int durationMinutes, Instant startedAt,
                         List<TestSection> sections, List<TestQuestionReference> references) {
    }

    @Provide
    Arbitrary<TimerScenario> scenarios() {
        Arbitrary<Integer> durations = Arbitraries.integers().between(1, 1440);
        // Fixed but varied start instants across a wide range of epoch seconds.
        Arbitrary<Instant> startedAts = Arbitraries.longs().between(0L, 4_000_000_000L)
                .map(Instant::ofEpochSecond);
        // 0..5 sections; capturing "zero sections" exercises the ungrouped case (Req 7.8).
        Arbitrary<Integer> sectionCounts = Arbitraries.integers().between(0, 5);

        return Combinators.combine(durations, startedAts, sectionCounts)
                .flatAs((duration, startedAt, sectionCount) -> {
                    List<TestSection> sections = new ArrayList<>();
                    List<Long> sectionIds = new ArrayList<>();
                    for (int i = 0; i < sectionCount; i++) {
                        TestSection section = new TestSection();
                        long sectionId = i + 1L;
                        section.setId(sectionId);
                        section.setTestId(TEST_ID);
                        section.setLabel("Section " + sectionId);
                        section.setPosition(i);
                        sections.add(section);
                        sectionIds.add(sectionId);
                    }
                    // References group under a section (or stay ungrouped when sectionId is null).
                    Arbitrary<List<TestQuestionReference>> refs = referencesArbitrary(sectionIds);
                    return refs.map(refList ->
                            new TimerScenario(duration, startedAt, sections, refList));
                });
    }

    private Arbitrary<List<TestQuestionReference>> referencesArbitrary(List<Long> sectionIds) {
        // null (ungrouped) is always allowed; grouped references pick one of the section ids.
        List<Long> sectionChoices = new ArrayList<>();
        sectionChoices.add(null);
        sectionChoices.addAll(sectionIds);

        Arbitrary<TestQuestionReference> oneRef = Combinators.combine(
                        Arbitraries.longs().between(1L, 10_000L),
                        Arbitraries.of(sectionChoices),
                        Arbitraries.integers().between(0, 20))
                .as((bankQuestionId, sectionId, position) -> {
                    TestQuestionReference ref = new TestQuestionReference();
                    ref.setTestId(TEST_ID);
                    ref.setBankQuestionId(bankQuestionId);
                    ref.setSectionId(sectionId);
                    ref.setPosition(position);
                    return ref;
                });
        return oneRef.list().ofMinSize(0).ofMaxSize(12);
    }
}
