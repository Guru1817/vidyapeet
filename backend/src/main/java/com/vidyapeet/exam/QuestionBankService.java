package com.vidyapeet.exam;

import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.dto.QuestionRequest;
import com.vidyapeet.exam.dto.QuestionResponse;
import com.vidyapeet.exam.dto.SectionRequest;
import com.vidyapeet.exam.dto.SectionResponse;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.exam.repository.TestSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * INSTITUTE_ADMIN management of the per-institute Question Bank and the
 * reuse-by-reference links that attach bank questions to tests.
 *
 * <p>Bank CRUD edits questions <em>in place</em> — there is no content copy — so any
 * edit to a bank question is reflected in every test that references it (Req 6.4).
 * Attaching creates a {@link TestQuestionReference}; detaching removes only that
 * reference row, retaining the bank question in the bank (Req 6.9). All answer
 * encoding/comparison continues to route through {@link AnswerCodec} exactly as the
 * existing per-test editor flow does. Tenant isolation is enforced by the
 * {@code @Filter} on {@link Question}/{@link TestQuestionReference} and the tenant-safe
 * {@code findById} on the repositories (Req 6.1, 6.6).
 */
@Service
public class QuestionBankService {

    private final QuestionRepository questionRepository;
    private final TestQuestionReferenceRepository referenceRepository;
    private final TestSectionRepository sectionRepository;
    private final MockTestRepository testRepository;

    public QuestionBankService(
            QuestionRepository questionRepository,
            TestQuestionReferenceRepository referenceRepository,
            TestSectionRepository sectionRepository,
            MockTestRepository testRepository) {
        this.questionRepository = questionRepository;
        this.referenceRepository = referenceRepository;
        this.sectionRepository = sectionRepository;
        this.testRepository = testRepository;
    }

    // ---------------------------------------------------------------------
    // Bank question CRUD (edits questions in place; no content copy)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<QuestionResponse> listBankQuestions() {
        return questionRepository.findAll().stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse getBankQuestion(Long id) {
        return QuestionResponse.from(requireBankQuestion(id));
    }

    @Transactional
    public QuestionResponse createBankQuestion(QuestionRequest request) {
        Question question = applyRequest(new Question(), request);
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse updateBankQuestion(Long id, QuestionRequest request) {
        Question question = requireBankQuestion(id);
        applyRequest(question, request);
        question = questionRepository.save(question);
        // Editing in place changes marks/answers for every referencing test, so keep
        // each referencing test's cached total marks in sync (Req 6.4).
        recomputeReferencingTestTotals(id);
        return QuestionResponse.from(question);
    }

    @Transactional
    public void deleteBankQuestion(Long id) {
        Question question = requireBankQuestion(id);
        if (!referenceRepository.findByBankQuestionId(id).isEmpty()) {
            throw Exceptions.conflict(
                    "This bank question is referenced by one or more tests. Detach it from every test before deleting.");
        }
        questionRepository.delete(question);
    }

    // ---------------------------------------------------------------------
    // Attach / detach references
    // ---------------------------------------------------------------------

    /** Attaches a bank question to a test at the next position; convenience overload. */
    @Transactional
    public void attachReference(Long testId, Long bankQuestionId) {
        attachReference(testId, bankQuestionId, null);
    }

    /**
     * Creates a {@link TestQuestionReference} linking the bank question to the test at
     * the next position. Rejects duplicates (uk_test_question_ref) with a conflict and
     * validates that both the test and the bank question exist within the institute.
     */
    @Transactional
    public void attachReference(Long testId, Long bankQuestionId, Long sectionId) {
        requireTest(testId);
        requireBankQuestion(bankQuestionId);
        if (referenceRepository.existsByTestIdAndBankQuestionId(testId, bankQuestionId)) {
            throw Exceptions.conflict("This question is already attached to the test.");
        }
        TestQuestionReference reference = new TestQuestionReference();
        reference.setTestId(testId);
        reference.setBankQuestionId(bankQuestionId);
        reference.setSectionId(sectionId);
        reference.setPosition((int) referenceRepository.countByTestId(testId));
        referenceRepository.save(reference);
        recomputeTotalMarks(testId);
    }

    /**
     * Removes only the reference row that links the bank question to the test; the bank
     * question itself is retained in the Question Bank (Req 6.9).
     */
    @Transactional
    public void detachReference(Long testId, Long bankQuestionId) {
        requireTest(testId);
        referenceRepository.deleteByTestIdAndBankQuestionId(testId, bankQuestionId);
        recomputeTotalMarks(testId);
    }

    // ---------------------------------------------------------------------
    // Test sections (labeled, ordered groupings under one overall timer — Req 7)
    // ---------------------------------------------------------------------
    //
    // Sections are organizational only: they carry no timing of their own. The overall
    // timer stays derived from startedAt + durationMinutes (Req 7.4); nothing here adds a
    // per-section limit. Tenant isolation is enforced by the @Filter on TestSection and the
    // tenant-safe findById on the repositories (Req 7.9).

    /** Sections of a test in display order (Req 7.1). */
    @Transactional(readOnly = true)
    public List<SectionResponse> listSections(Long testId) {
        requireTest(testId);
        return sectionRepository.findByTestIdOrderByPositionAsc(testId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    /**
     * Creates a labeled section for a test. When {@code position} is omitted the section is
     * appended after the existing ones.
     */
    @Transactional
    public SectionResponse createSection(Long testId, SectionRequest request) {
        requireTest(testId);
        TestSection section = new TestSection();
        section.setTestId(testId);
        section.setLabel(request.label());
        section.setPosition(request.position() != null
                ? request.position()
                : sectionRepository.findByTestIdOrderByPositionAsc(testId).size());
        return SectionResponse.from(sectionRepository.save(section));
    }

    /** Renames and/or reorders a section; a null {@code position} leaves the order unchanged. */
    @Transactional
    public SectionResponse updateSection(Long testId, Long sectionId, SectionRequest request) {
        TestSection section = requireSection(testId, sectionId);
        section.setLabel(request.label());
        if (request.position() != null) {
            section.setPosition(request.position());
        }
        return SectionResponse.from(sectionRepository.save(section));
    }

    /**
     * Deletes a section. Any reference grouped under it is moved back to the ungrouped list
     * (its {@code section_id} is set to {@code null}) first, so no reference is left pointing
     * at a removed section (avoids an orphan FK — Req 7.8 ungrouped fallback).
     */
    @Transactional
    public void deleteSection(Long testId, Long sectionId) {
        TestSection section = requireSection(testId, sectionId);
        referenceRepository.findByTestIdOrderBySectionPositionAscPositionAsc(testId).stream()
                .filter(reference -> sectionId.equals(reference.getSectionId()))
                .forEach(reference -> {
                    reference.setSectionId(null);
                    referenceRepository.save(reference);
                });
        sectionRepository.delete(section);
    }

    /**
     * Groups (or ungroups) a bank question's reference within a test under a section by
     * setting {@code TestQuestionReference.section_id}. A null {@code sectionId} ungroups the
     * reference. Validates that both the reference and (when given) the section belong to the
     * test.
     */
    @Transactional
    public void assignReferenceToSection(Long testId, Long bankQuestionId, Long sectionId) {
        requireTest(testId);
        TestQuestionReference reference = referenceRepository
                .findByTestIdAndBankQuestionId(testId, bankQuestionId)
                .orElseThrow(() -> Exceptions.notFound(
                        "No reference found for question " + bankQuestionId + " on this test."));
        if (sectionId != null) {
            requireSection(testId, sectionId);
        }
        reference.setSectionId(sectionId);
        referenceRepository.save(reference);
    }

    // ---------------------------------------------------------------------
    // Shared answer encoding (identical to the existing per-test editor flow)
    // ---------------------------------------------------------------------

    /**
     * Applies a create/update request onto a bank question, encoding the correct answer
     * into its canonical {@link AnswerCodec} form. Shared by {@link ExamService}'s
     * per-test add/import flow so both paths encode answers identically.
     */
    Question applyRequest(Question question, QuestionRequest request) {
        question.setType(request.type());
        question.setText(request.text());
        question.setMarks(request.marks());

        switch (request.type()) {
            case MCQ -> {
                requireOptions(request);
                if (request.correctOption() == null) {
                    throw Exceptions.badRequest("Select the correct option for an MCQ question.");
                }
                setOptions(question, request);
                question.setCorrectAnswer(request.correctOption().name());
            }
            case MSQ -> {
                requireOptions(request);
                if (request.correctOptions() == null || request.correctOptions().isEmpty()) {
                    throw Exceptions.badRequest("Select at least one correct option for an MSQ question.");
                }
                setOptions(question, request);
                question.setCorrectAnswer(AnswerCodec.encodeOptions(request.correctOptions()));
            }
            case TRUE_FALSE -> {
                if (request.correctBoolean() == null) {
                    throw Exceptions.badRequest("Specify whether the statement is true or false.");
                }
                clearOptions(question);
                question.setCorrectAnswer(request.correctBoolean() ? "TRUE" : "FALSE");
            }
            case FILL_BLANK -> {
                List<String> accepted = request.acceptedAnswers() == null
                        ? List.of()
                        : request.acceptedAnswers().stream().filter(StringUtils::hasText).toList();
                if (accepted.isEmpty()) {
                    throw Exceptions.badRequest("Provide at least one accepted answer for a fill-in-the-blank question.");
                }
                clearOptions(question);
                question.setCorrectAnswer(AnswerCodec.encodeAccepted(accepted));
            }
        }
        return question;
    }

    private void requireOptions(QuestionRequest request) {
        if (!StringUtils.hasText(request.optionA()) || !StringUtils.hasText(request.optionB())
                || !StringUtils.hasText(request.optionC()) || !StringUtils.hasText(request.optionD())) {
            throw Exceptions.badRequest("All four options are required for MCQ/MSQ questions.");
        }
    }

    private void setOptions(Question question, QuestionRequest request) {
        question.setOptionA(request.optionA());
        question.setOptionB(request.optionB());
        question.setOptionC(request.optionC());
        question.setOptionD(request.optionD());
    }

    private void clearOptions(Question question) {
        question.setOptionA(null);
        question.setOptionB(null);
        question.setOptionC(null);
        question.setOptionD(null);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Recomputes cached total marks for every test that references the given bank question. */
    private void recomputeReferencingTestTotals(Long bankQuestionId) {
        referenceRepository.findByBankQuestionId(bankQuestionId).stream()
                .map(TestQuestionReference::getTestId)
                .distinct()
                .forEach(this::recomputeTotalMarks);
    }

    private void recomputeTotalMarks(Long testId) {
        int total = referenceRepository.findResolvedQuestions(testId).stream()
                .mapToInt(Question::getMarks)
                .sum();
        MockTest test = requireTest(testId);
        test.setTotalMarks(total);
        testRepository.save(test);
    }

    private MockTest requireTest(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No test found with id " + id + "."));
    }

    /** Loads a section and verifies it belongs to the given test (tenant-safe findById). */
    private TestSection requireSection(Long testId, Long sectionId) {
        TestSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> Exceptions.notFound("No section found with id " + sectionId + "."));
        if (!section.getTestId().equals(testId)) {
            throw Exceptions.notFound("No section found with id " + sectionId + " for this test.");
        }
        return section;
    }

    private Question requireBankQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No question found with id " + id + "."));
    }
}
