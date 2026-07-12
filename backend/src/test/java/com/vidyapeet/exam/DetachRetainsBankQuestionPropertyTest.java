package com.vidyapeet.exam;

import com.vidyapeet.exam.dto.QuestionResponse;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 9: Detaching a reference retains the bank question
 *
 * <p>For any bank question referenced by one or more tests, removing its reference from a
 * single test deletes only that reference row: the bank question still exists in the bank
 * and every other test's reference to it remains intact.
 *
 * <p>Validates: Requirements 6.9
 *
 * <p>The property attaches one bank question to {@code N} distinct tests through the real
 * {@link QuestionBankService#attachReference} path, detaches it from exactly one test via
 * {@link QuestionBankService#detachReference}, then asserts the detached reference is gone,
 * the other {@code N - 1} references remain, and the bank question is still resolvable
 * (both {@code findById} and the bank listing). Repositories are backed by simple in-memory
 * fakes so the reference deletion and bank retention are observed exactly as they would be
 * against the database, with no Supabase or live datasource touched across 100+ iterations.
 */
class DetachRetainsBankQuestionPropertyTest {

    private static final long BANK_QUESTION_ID = 42L;

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    void detachingRemovesOnlyTheReferenceRowAndRetainsTheBankQuestion(
            @ForAll("testCounts") int testCount) {

        // ----- In-memory persisted state -------------------------------------------------
        // A single bank question shared across every test.
        Question bankQuestion = new Question();
        bankQuestion.setId(BANK_QUESTION_ID);
        bankQuestion.setType(QuestionType.MCQ);
        bankQuestion.setText("Shared bank question");
        bankQuestion.setMarks(3);

        // References live in this list; the fake repository mutates it just like the DB row set.
        List<TestQuestionReference> references = new ArrayList<>();
        AtomicLong referenceIds = new AtomicLong(1L);

        QuestionRepository questionRepository = mock(QuestionRepository.class);
        when(questionRepository.findById(BANK_QUESTION_ID)).thenReturn(Optional.of(bankQuestion));
        when(questionRepository.findAll()).thenReturn(List.of(bankQuestion));

        MockTestRepository testRepository = mock(MockTestRepository.class);
        when(testRepository.findById(anyLong())).thenAnswer(invocation -> {
            MockTest test = new MockTest();
            test.setId(invocation.getArgument(0));
            test.setTitle("Test " + invocation.getArgument(0));
            test.setDurationMinutes(60);
            return Optional.of(test);
        });
        when(testRepository.save(any(MockTest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestQuestionReferenceRepository referenceRepository = mock(TestQuestionReferenceRepository.class);
        when(referenceRepository.save(any(TestQuestionReference.class))).thenAnswer(invocation -> {
            TestQuestionReference ref = invocation.getArgument(0);
            if (ref.getId() == null) {
                ref.setId(referenceIds.getAndIncrement());
            }
            references.add(ref);
            return ref;
        });
        when(referenceRepository.existsByTestIdAndBankQuestionId(anyLong(), anyLong()))
                .thenAnswer(invocation -> references.stream().anyMatch(r ->
                        r.getTestId().equals(invocation.getArgument(0))
                                && r.getBankQuestionId().equals(invocation.getArgument(1))));
        when(referenceRepository.countByTestId(anyLong()))
                .thenAnswer(invocation -> references.stream()
                        .filter(r -> r.getTestId().equals(invocation.getArgument(0)))
                        .count());
        when(referenceRepository.findByBankQuestionId(anyLong()))
                .thenAnswer(invocation -> references.stream()
                        .filter(r -> r.getBankQuestionId().equals(invocation.getArgument(0)))
                        .toList());
        when(referenceRepository.findResolvedQuestions(anyLong()))
                .thenAnswer(invocation -> references.stream()
                        .filter(r -> r.getTestId().equals(invocation.getArgument(0)))
                        .map(r -> bankQuestion)
                        .toList());
        // deleteByTestIdAndBankQuestionId is void: remove matching rows from the fake state.
        org.mockito.Mockito.doAnswer(invocation -> {
            references.removeIf(r -> r.getTestId().equals(invocation.getArgument(0))
                    && r.getBankQuestionId().equals(invocation.getArgument(1)));
            return null;
        }).when(referenceRepository).deleteByTestIdAndBankQuestionId(anyLong(), anyLong());

        TestSectionRepository sectionRepository = mock(TestSectionRepository.class);

        QuestionBankService service = new QuestionBankService(
                questionRepository, referenceRepository, sectionRepository, testRepository);

        // ----- Attach the one bank question to N distinct tests ---------------------------
        List<Long> testIds = new ArrayList<>();
        for (int i = 0; i < testCount; i++) {
            long testId = 1000L + i;
            testIds.add(testId);
            service.attachReference(testId, BANK_QUESTION_ID);
        }
        assertThat(references).hasSize(testCount);

        // ----- Detach from exactly one test ----------------------------------------------
        long detachedTestId = testIds.get(testCount / 2);
        service.detachReference(detachedTestId, BANK_QUESTION_ID);

        // Only that one reference row is gone.
        assertThat(referenceRepository.existsByTestIdAndBankQuestionId(detachedTestId, BANK_QUESTION_ID))
                .as("detached reference should be removed")
                .isFalse();

        // Every other test's reference to the bank question remains intact.
        List<TestQuestionReference> remaining = referenceRepository.findByBankQuestionId(BANK_QUESTION_ID);
        assertThat(remaining).hasSize(testCount - 1);
        assertThat(remaining).extracting(TestQuestionReference::getTestId)
                .doesNotContain(detachedTestId)
                .containsExactlyInAnyOrderElementsOf(
                        testIds.stream().filter(id -> id != detachedTestId).toList());

        // The bank question itself still exists in the bank.
        assertThat(questionRepository.findById(BANK_QUESTION_ID)).isPresent();
        assertThat(service.getBankQuestion(BANK_QUESTION_ID).id()).isEqualTo(BANK_QUESTION_ID);
        assertThat(service.listBankQuestions())
                .extracting(QuestionResponse::id)
                .contains(BANK_QUESTION_ID);
    }

    @Provide
    Arbitrary<Integer> testCounts() {
        return Arbitraries.integers().between(1, 12);
    }
}
