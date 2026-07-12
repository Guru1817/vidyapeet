package com.vidyapeet.exam;

import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 6: Attaching references without copying
 *
 * <p>For any bank question and any set of tests, attaching the question to those tests
 * creates one {@link TestQuestionReference} per test that all point to the same unchanged
 * bank question id, without creating additional bank question rows.
 *
 * <p>Validates: Requirements 6.2, 6.3
 *
 * <p>Mirrors the existing backend property-test style (Mockito-backed repositories over an
 * in-memory store, as in {@code QuestionImageAssociationPropertyTest} and
 * {@code ThemePersistenceRoundTripPropertyTest}) so real {@link QuestionBankService} logic
 * is exercised without a database. A single seeded bank question row backs
 * {@code questionRepository}; the reference repository records saved references in a list.
 * After attaching the one bank question to N generated tests we assert there are exactly N
 * references, all carrying the same {@code bankQuestionId}, and that no additional bank
 * question row was ever created (reuse-by-reference, not content copy).
 */
class AttachReferenceWithoutCopyingPropertyTest {

    @Property(tries = 100)
    void attachingCreatesOneReferencePerTestPointingAtTheSameUnchangedBankQuestion(
            @ForAll("scenarios") AttachScenario scenario) {

        Long bankQuestionId = scenario.bankQuestionId();
        List<Long> testIds = scenario.testIds();

        // ---- In-memory bank: exactly one seeded bank question row. --------------------
        Question bankQuestion = new Question();
        bankQuestion.setId(bankQuestionId);
        bankQuestion.setType(QuestionType.MCQ);
        bankQuestion.setText("What is 2 + 2?");
        bankQuestion.setMarks(4);
        bankQuestion.setCorrectAnswer("A");

        Map<Long, Question> bankStore = new HashMap<>();
        bankStore.put(bankQuestionId, bankQuestion);

        QuestionRepository questionRepository = mock(QuestionRepository.class);
        when(questionRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(bankStore.get(inv.<Long>getArgument(0))));
        // Any save would represent a new/changed bank row; record it so we can prove none happen.
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            bankStore.put(q.getId(), q);
            return q;
        });

        // ---- In-memory reference store. -----------------------------------------------
        List<TestQuestionReference> references = new ArrayList<>();
        TestQuestionReferenceRepository referenceRepository = mock(TestQuestionReferenceRepository.class);
        when(referenceRepository.existsByTestIdAndBankQuestionId(anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    Long testId = inv.getArgument(0);
                    Long bqId = inv.getArgument(1);
                    return references.stream()
                            .anyMatch(r -> r.getTestId().equals(testId) && r.getBankQuestionId().equals(bqId));
                });
        when(referenceRepository.countByTestId(anyLong()))
                .thenAnswer(inv -> references.stream()
                        .filter(r -> r.getTestId().equals(inv.<Long>getArgument(0)))
                        .count());
        when(referenceRepository.save(any(TestQuestionReference.class)))
                .thenAnswer(inv -> {
                    TestQuestionReference r = inv.getArgument(0);
                    references.add(r);
                    return r;
                });
        // Used by recomputeTotalMarks after each attach: resolve the bank questions for a test.
        when(referenceRepository.findResolvedQuestions(anyLong()))
                .thenAnswer(inv -> references.stream()
                        .filter(r -> r.getTestId().equals(inv.<Long>getArgument(0)))
                        .map(r -> bankStore.get(r.getBankQuestionId()))
                        .toList());

        // ---- In-memory tests: each generated test id resolves to a fresh MockTest. -----
        MockTestRepository testRepository = mock(MockTestRepository.class);
        when(testRepository.findById(anyLong())).thenAnswer(inv -> {
            MockTest test = new MockTest();
            test.setId(inv.getArgument(0));
            test.setTitle("Test " + inv.getArgument(0));
            test.setDurationMinutes(30);
            return Optional.of(test);
        });
        when(testRepository.save(any(MockTest.class))).thenAnswer(inv -> inv.getArgument(0));

        TestSectionRepository sectionRepository = mock(TestSectionRepository.class);

        QuestionBankService service = new QuestionBankService(
                questionRepository, referenceRepository, sectionRepository, testRepository);

        // ---- Act: attach the single bank question to every generated test. ------------
        for (Long testId : testIds) {
            service.attachReference(testId, bankQuestionId);
        }

        // ---- Assert. ------------------------------------------------------------------
        int expectedCount = testIds.size();

        // Exactly one reference per test.
        assertThat(references).hasSize(expectedCount);

        // Every reference points at the same, unchanged bank question id.
        assertThat(references)
                .allSatisfy(r -> assertThat(r.getBankQuestionId()).isEqualTo(bankQuestionId));

        // One reference for each distinct test (no duplicates, no test missed).
        assertThat(references.stream().map(TestQuestionReference::getTestId).distinct().toList())
                .containsExactlyInAnyOrderElementsOf(testIds);

        // No additional bank question rows were created — attaching references never copies content.
        verify(questionRepository, never()).save(any(Question.class));
        assertThat(bankStore).hasSize(1);
        assertThat(bankStore.get(bankQuestionId)).isSameAs(bankQuestion);
    }

    /**
     * A bank question id plus a non-empty set of distinct test ids to attach it to. Using a
     * set guarantees distinct tests so each attach targets a different test (no duplicate
     * conflicts) while still exercising the "referenced by multiple tests" case (Req 6.3).
     */
    @Provide
    Arbitrary<AttachScenario> scenarios() {
        Arbitrary<Long> bankQuestionIds = Arbitraries.longs().between(1L, 1_000L);
        Arbitrary<Set<Long>> testIdSets = Arbitraries.longs().between(1_001L, 100_000L)
                .set().ofMinSize(1).ofMaxSize(8);
        return net.jqwik.api.Combinators.combine(bankQuestionIds, testIdSets)
                .as((bankQuestionId, testIds) -> new AttachScenario(bankQuestionId, List.copyOf(testIds)));
    }

    /** One bank question attached to a set of distinct tests. */
    record AttachScenario(Long bankQuestionId, List<Long> testIds) {
    }
}
