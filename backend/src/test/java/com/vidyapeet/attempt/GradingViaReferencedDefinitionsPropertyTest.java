package com.vidyapeet.attempt;

import com.vidyapeet.exam.AnswerOption;
import com.vidyapeet.exam.MockTest;
import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.QuestionBankService;
import com.vidyapeet.exam.QuestionType;
import com.vidyapeet.exam.TestQuestionReference;
import com.vidyapeet.exam.dto.QuestionRequest;
import com.vidyapeet.exam.dto.QuestionResponse;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 8: Grading via referenced definitions
 *
 * <p>For any test whose questions are attached by reference and any set of student
 * selections, the per-question awarded marks and the total attempt score are exactly
 * those produced by the {@link Grader} over the resolved bank question definitions using
 * {@link com.vidyapeet.exam.AnswerCodec}, and the resulting score is a {@code double} that
 * equals the sum of awarded marks (including fractional and negative values under negative
 * marking).
 *
 * <p>Validates: Requirements 6.5, 6.8, 6.10, 8.2, 8.6
 *
 * <p>Mirrors the established backend property-test style (Mockito-backed in-memory
 * repositories, no Spring context, no database — as in {@code EditPropagationPropertyTest}
 * and {@code AttachReferenceWithoutCopyingPropertyTest}). The test exercises the real
 * resolve&rarr;grade path: bank questions are created through {@link QuestionBankService}
 * (canonical answers encoded via {@code AnswerCodec}), attached to a test by reference, then
 * resolved through {@link TestQuestionReferenceRepository#findResolvedQuestions} and graded by
 * the real {@link Grader}. Student selections are generated as correct / incorrect / blank per
 * question. An independent reference computation (driven by the generated selection intent, not
 * by re-reading {@code AnswerCodec}) predicts each question's awarded marks and the total, and
 * we assert the {@code Grader} output matches exactly and that {@code totalScore} equals the
 * sum of the per-question awarded marks.
 */
class GradingViaReferencedDefinitionsPropertyTest {

    private static final long TEST_ID = 1L;
    private final Grader grader = new Grader();

    enum SelectionKind {CORRECT, INCORRECT, BLANK}

    @Property(tries = 100)
    void gradingOverReferenceResolvedQuestionsMatchesGraderAndAnswerCodec(
            @ForAll("scenarios") GradeScenario scenario) {

        // --- In-memory backing state shared by the mocked repositories ------------------
        Map<Long, Question> questionStore = new HashMap<>();
        AtomicLong questionSeq = new AtomicLong(0);
        List<TestQuestionReference> referenceStore = new ArrayList<>();
        AtomicLong referenceSeq = new AtomicLong(0);
        Map<Long, MockTest> testStore = new HashMap<>();

        MockTest test = new MockTest();
        test.setId(TEST_ID);
        test.setTitle("Referenced Test");
        test.setDurationMinutes(30);
        test.setNegativeMarking(scenario.negativeMarking());
        test.setNegativeMarkPerWrong(scenario.negativeMarkPerWrong());
        testStore.put(TEST_ID, test);

        QuestionRepository questionRepository = mock(QuestionRepository.class);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            if (q.getId() == null) {
                q.setId(questionSeq.incrementAndGet());
            }
            questionStore.put(q.getId(), q);
            return q;
        });
        when(questionRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(questionStore.get(invocation.<Long>getArgument(0))));

        TestQuestionReferenceRepository referenceRepository = mock(TestQuestionReferenceRepository.class);
        when(referenceRepository.save(any(TestQuestionReference.class))).thenAnswer(invocation -> {
            TestQuestionReference ref = invocation.getArgument(0);
            if (ref.getId() == null) {
                ref.setId(referenceSeq.incrementAndGet());
            }
            referenceStore.add(ref);
            return ref;
        });
        when(referenceRepository.existsByTestIdAndBankQuestionId(anyLong(), anyLong()))
                .thenAnswer(invocation -> referenceStore.stream().anyMatch(r ->
                        r.getTestId().equals(invocation.<Long>getArgument(0))
                                && r.getBankQuestionId().equals(invocation.<Long>getArgument(1))));
        when(referenceRepository.countByTestId(anyLong()))
                .thenAnswer(invocation -> referenceStore.stream()
                        .filter(r -> r.getTestId().equals(invocation.<Long>getArgument(0)))
                        .count());
        // Resolution returns the live shared bank questions for a test, ordered by section
        // then position — exactly as the JPQL query does against the database.
        when(referenceRepository.findResolvedQuestions(anyLong()))
                .thenAnswer(invocation -> referenceStore.stream()
                        .filter(r -> r.getTestId().equals(invocation.<Long>getArgument(0)))
                        .sorted(Comparator
                                .comparing((TestQuestionReference r) ->
                                        r.getSectionId() == null ? Long.MIN_VALUE : r.getSectionId())
                                .thenComparing(TestQuestionReference::getPosition))
                        .map(r -> questionStore.get(r.getBankQuestionId()))
                        .toList());

        MockTestRepository testRepository = mock(MockTestRepository.class);
        when(testRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(testStore.get(invocation.<Long>getArgument(0))));
        when(testRepository.save(any(MockTest.class))).thenAnswer(invocation -> {
            MockTest t = invocation.getArgument(0);
            testStore.put(t.getId(), t);
            return t;
        });

        TestSectionRepository sectionRepository = mock(TestSectionRepository.class);

        QuestionBankService bankService = new QuestionBankService(
                questionRepository, referenceRepository, sectionRepository, testRepository);

        // --- Arrange: create each bank question, attach it by reference, record intent ---
        // intentById preserves per-question the generated selection kind so we can build an
        // independent expectation of the awarded marks.
        Map<Long, SelectionKind> intentById = new LinkedHashMap<>();
        Map<Long, String> selections = new HashMap<>();
        for (QSpec spec : scenario.specs()) {
            QuestionResponse created = bankService.createBankQuestion(spec.request());
            Long questionId = created.id();
            bankService.attachReference(TEST_ID, questionId);

            intentById.put(questionId, spec.kind());
            String selected = selectionFor(spec.request(), spec.kind());
            if (selected != null) {
                selections.put(questionId, selected);
            }
        }

        // --- Act: resolve the test's questions by reference and grade them ---------------
        List<Question> resolved = referenceRepository.findResolvedQuestions(TEST_ID);
        GradeOutcome outcome = grader.grade(
                resolved, selections, test.isNegativeMarking(), test.getNegativeMarkPerWrong());

        // --- Independent expectation over the SAME resolved order ------------------------
        double penalty = -Math.abs(scenario.negativeMarkPerWrong());
        Map<Long, Double> expectedAwardById = new HashMap<>();
        Map<Long, Boolean> expectedCorrectById = new HashMap<>();
        double expectedTotal = 0.0;
        for (Question q : resolved) {
            SelectionKind kind = intentById.get(q.getId());
            boolean expectedCorrect = kind == SelectionKind.CORRECT;
            double awarded;
            if (kind == SelectionKind.CORRECT) {
                awarded = q.getMarks();
            } else if (kind == SelectionKind.INCORRECT && scenario.negativeMarking()) {
                awarded = penalty;
            } else {
                awarded = 0.0;
            }
            expectedTotal += awarded;
            expectedAwardById.put(q.getId(), awarded);
            expectedCorrectById.put(q.getId(), expectedCorrect);
        }

        // --- Assert: per-question grading matches the resolved bank definitions ----------
        assertThat(outcome.answers()).hasSameSizeAs(resolved);
        for (GradedAnswer graded : outcome.answers()) {
            assertThat(graded.correct())
                    .as("correctness for question %s", graded.questionId())
                    .isEqualTo(expectedCorrectById.get(graded.questionId()));
            assertThat(graded.marksAwarded())
                    .as("awarded marks for question %s", graded.questionId())
                    .isEqualTo(expectedAwardById.get(graded.questionId()));
        }

        // Total attempt score equals the independent expectation ...
        assertThat(outcome.totalScore()).isEqualTo(expectedTotal);

        // ... and is a double equal to the sum of the per-question awarded marks (in the
        // grading order), including fractional and negative values under negative marking.
        double sumOfAwarded = 0.0;
        for (GradedAnswer graded : outcome.answers()) {
            sumOfAwarded += graded.marksAwarded();
        }
        assertThat(outcome.totalScore()).isEqualTo(sumOfAwarded);
    }

    /**
     * Builds a student selection string for a question, guaranteed to be correct, incorrect,
     * or blank per {@code AnswerCodec} semantics:
     * <ul>
     *   <li>CORRECT selections deliberately vary formatting (option-set reordering,
     *       case/whitespace on fill-blank) to exercise canonical comparison.</li>
     *   <li>INCORRECT selections are constructed to never match the canonical answer.</li>
     *   <li>BLANK returns {@code null} (an unanswered question).</li>
     * </ul>
     */
    private static String selectionFor(QuestionRequest r, SelectionKind kind) {
        if (kind == SelectionKind.BLANK) {
            return null;
        }
        boolean correct = kind == SelectionKind.CORRECT;
        return switch (r.type()) {
            case MCQ -> correct ? r.correctOption().name() : otherOption(r.correctOption()).name();
            case MSQ -> {
                Set<AnswerOption> correctSet = new TreeSet<>(r.correctOptions());
                if (correct) {
                    // Reverse the canonical order to exercise order-insensitive comparison.
                    List<AnswerOption> reversed = new ArrayList<>(correctSet);
                    java.util.Collections.reverse(reversed);
                    yield reversed.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElseThrow();
                }
                yield csv(differentNonEmptySet(correctSet));
            }
            case TRUE_FALSE -> {
                boolean value = r.correctBoolean();
                boolean chosen = correct == value; // flip when we want an incorrect answer
                yield chosen ? "TRUE" : "FALSE";
            }
            case FILL_BLANK -> correct
                    // trim + case are normalised by AnswerCodec, so decorate the accepted answer.
                    ? "  " + r.acceptedAnswers().get(0).toUpperCase() + " "
                    // Longer than any generated accepted answer (max length 10) => never matches.
                    : "notacceptedanswerzzz";
        };
    }

    private static AnswerOption otherOption(AnswerOption correct) {
        for (AnswerOption o : AnswerOption.values()) {
            if (o != correct) {
                return o;
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /** A non-empty option subset guaranteed to differ from {@code correctSet}. */
    private static Set<AnswerOption> differentNonEmptySet(Set<AnswerOption> correctSet) {
        for (AnswerOption o : AnswerOption.values()) {
            if (!correctSet.contains(o)) {
                Set<AnswerOption> wrong = new TreeSet<>(correctSet);
                wrong.add(o); // adding a missing option yields a different, non-empty set
                return wrong;
            }
        }
        // correctSet is the full {A,B,C,D}; {A} differs and is non-empty.
        return EnumSet.of(AnswerOption.A);
    }

    private static String csv(Set<AnswerOption> options) {
        return options.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElseThrow();
    }

    // -------------------------------------------------------------------------
    // Generators
    // -------------------------------------------------------------------------

    /** A referenced test's questions plus the negative-marking configuration. */
    record GradeScenario(List<QSpec> specs, boolean negativeMarking, double negativeMarkPerWrong) {
    }

    /** A single bank question definition together with the student's selection intent. */
    record QSpec(QuestionRequest request, SelectionKind kind) {
    }

    @Provide
    Arbitrary<GradeScenario> scenarios() {
        Arbitrary<List<QSpec>> specs = questionSpecs().list().ofMinSize(1).ofMaxSize(8);
        Arbitrary<Boolean> negativeMarking = Arbitraries.of(true, false);
        // Range spans negative inputs (exercising Math.abs) and fractional values so totals
        // include fractional and negative outcomes under negative marking.
        Arbitrary<Double> negativeMarkPerWrong = Arbitraries.doubles().between(-3.0, 3.0);
        return Combinators.combine(specs, negativeMarking, negativeMarkPerWrong).as(GradeScenario::new);
    }

    @Provide
    Arbitrary<QSpec> questionSpecs() {
        Arbitrary<SelectionKind> kinds = Arbitraries.of(SelectionKind.values());
        return Combinators.combine(questionRequests(), kinds).as(QSpec::new);
    }

    /** A valid create payload for a randomly chosen question type with a canonical answer. */
    @Provide
    Arbitrary<QuestionRequest> questionRequests() {
        return Arbitraries.of(QuestionType.values()).flatMap(this::requestForType);
    }

    private Arbitrary<QuestionRequest> requestForType(QuestionType type) {
        Arbitrary<String> texts = Arbitraries.strings()
                .withCharRange('a', 'z').withChars(' ')
                .ofMinLength(1).ofMaxLength(40)
                .filter(s -> !s.isBlank());
        Arbitrary<Integer> marks = Arbitraries.integers().between(1, 100);
        Arbitrary<String> options = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1).ofMaxLength(10)
                .filter(s -> !s.isBlank());

        return switch (type) {
            case MCQ -> Combinators.combine(
                            texts, marks, options, options, options, options,
                            Arbitraries.of(AnswerOption.values()))
                    .as((text, mark, a, b, c, d, correct) -> new QuestionRequest(
                            QuestionType.MCQ, text, a, b, c, d, correct, null, null, null, mark));
            case MSQ -> Combinators.combine(
                            texts, marks, options, options, options, options,
                            Arbitraries.of(AnswerOption.values()).set().ofMinSize(1).ofMaxSize(4)
                                    .map(ArrayList::new))
                    .as((text, mark, a, b, c, d, correctOptions) -> new QuestionRequest(
                            QuestionType.MSQ, text, a, b, c, d, null,
                            new ArrayList<>(correctOptions), null, null, mark));
            case TRUE_FALSE -> Combinators.combine(texts, marks, Arbitraries.of(true, false))
                    .as((text, mark, bool) -> new QuestionRequest(
                            QuestionType.TRUE_FALSE, text, null, null, null, null,
                            null, null, bool, null, mark));
            case FILL_BLANK -> Combinators.combine(
                            texts, marks,
                            options.list().ofMinSize(1).ofMaxSize(3))
                    .as((text, mark, accepted) -> new QuestionRequest(
                            QuestionType.FILL_BLANK, text, null, null, null, null,
                            null, null, null, accepted, mark));
        };
    }
}
