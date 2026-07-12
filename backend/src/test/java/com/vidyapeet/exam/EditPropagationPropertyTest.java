package com.vidyapeet.exam;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 7: Editing a bank question reflects in every referencing test
 *
 * <p>For any bank question referenced by any number of tests, editing the bank question
 * causes every referencing test to resolve the updated content (text, options, canonical
 * correct answer, marks, image).
 *
 * <p>Validates: Requirements 6.4
 *
 * <p>The test exercises the real {@link QuestionBankService#updateBankQuestion} edit-in-place
 * path against an in-memory Mockito-backed repository set (matching the established backend
 * property-test style — no Spring context, no database). A single bank question is attached
 * to {@code N} distinct tests via {@link QuestionBankService#attachReference}; after the edit,
 * each test's questions are resolved through
 * {@link TestQuestionReferenceRepository#findResolvedQuestions} and asserted to carry the
 * edited definition. Because reuse-by-reference stores one shared {@link Question} (no per-test
 * copy), every referencing test must observe the same updated content.
 */
class EditPropagationPropertyTest {

    @Property(tries = 100)
    void editingABankQuestionReflectsInEveryReferencingTest(@ForAll("scenarios") EditScenario scenario) {
        // --- In-memory backing state shared by the mocked repositories ---------------
        Map<Long, Question> questionStore = new HashMap<>();
        AtomicLong questionSeq = new AtomicLong(0);
        List<TestQuestionReference> referenceStore = new ArrayList<>();
        AtomicLong referenceSeq = new AtomicLong(0);
        Map<Long, MockTest> testStore = new HashMap<>();

        // Pre-create the tests that will reference the bank question.
        for (Long testId : scenario.testIds()) {
            MockTest test = new MockTest();
            test.setId(testId);
            test.setTitle("Test " + testId);
            test.setDurationMinutes(30);
            testStore.put(testId, test);
        }

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
        when(referenceRepository.findByBankQuestionId(anyLong()))
                .thenAnswer(invocation -> referenceStore.stream()
                        .filter(r -> r.getBankQuestionId().equals(invocation.<Long>getArgument(0)))
                        .toList());
        // Resolution returns the live shared bank question entities for a test, ordered
        // by section then position — exactly as the JPQL query does against the database.
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

        QuestionBankService service = new QuestionBankService(
                questionRepository, referenceRepository, sectionRepository, testRepository);

        // --- Arrange: create the bank question and attach it to every test -----------
        QuestionResponse created = service.createBankQuestion(scenario.initial());
        Long bankQuestionId = created.id();
        // Simulate an attached image on the shared bank question (image is set out-of-band
        // via the image upload flow, not through the edit request).
        questionStore.get(bankQuestionId).setImageKey(scenario.imageKey());

        for (Long testId : scenario.testIds()) {
            service.attachReference(testId, bankQuestionId);
        }

        // --- Act: edit the shared bank question in place -----------------------------
        service.updateBankQuestion(bankQuestionId, scenario.edited());

        // Expected canonical definition after the edit (image is preserved by the edit).
        Question expected = new Question();
        service.applyRequest(expected, scenario.edited());

        // --- Assert: EVERY referencing test resolves the updated content -------------
        for (Long testId : scenario.testIds()) {
            List<Question> resolved = referenceRepository.findResolvedQuestions(testId);
            Question q = resolved.stream()
                    .filter(x -> x.getId().equals(bankQuestionId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Test " + testId + " lost its reference to the edited bank question"));

            assertThat(q.getType()).isEqualTo(expected.getType());
            assertThat(q.getText()).isEqualTo(expected.getText());
            assertThat(q.getCorrectAnswer()).isEqualTo(expected.getCorrectAnswer());
            assertThat(q.getMarks()).isEqualTo(expected.getMarks());
            assertThat(q.getOptionA()).isEqualTo(expected.getOptionA());
            assertThat(q.getOptionB()).isEqualTo(expected.getOptionB());
            assertThat(q.getOptionC()).isEqualTo(expected.getOptionC());
            assertThat(q.getOptionD()).isEqualTo(expected.getOptionD());
            // The attached image is part of the shared definition and is reflected too.
            assertThat(q.getImageKey()).isEqualTo(scenario.imageKey());
        }
    }

    // -------------------------------------------------------------------------
    // Generators
    // -------------------------------------------------------------------------

    /**
     * An edit scenario: an initial bank-question definition, a (possibly different-typed)
     * edited definition, the set of tests referencing the question, and an optional image
     * key attached to the shared question.
     */
    record EditScenario(QuestionRequest initial, QuestionRequest edited, List<Long> testIds, String imageKey) {
    }

    @Provide
    Arbitrary<EditScenario> scenarios() {
        Arbitrary<List<Long>> testIds = Arbitraries.longs().between(1L, 100_000L)
                .set().ofMinSize(1).ofMaxSize(6)
                .map(ArrayList::new);
        Arbitrary<String> imageKeys = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(4).ofMaxLength(12)
                        .map(s -> "images/" + s + ".png"));

        return Combinators.combine(questionRequests(), questionRequests(), testIds, imageKeys)
                .as(EditScenario::new);
    }

    /** A valid create/update payload for a randomly chosen question type. */
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
