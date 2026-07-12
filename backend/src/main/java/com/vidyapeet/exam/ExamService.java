package com.vidyapeet.exam;

import com.vidyapeet.attempt.TestAttempt;
import com.vidyapeet.attempt.repository.AttemptAnswerRepository;
import com.vidyapeet.attempt.repository.TestAttemptRepository;
import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.dto.CreateTestRequest;
import com.vidyapeet.exam.dto.QuestionRequest;
import com.vidyapeet.exam.dto.QuestionResponse;
import com.vidyapeet.exam.dto.TestDetailResponse;
import com.vidyapeet.exam.dto.TestResponse;
import com.vidyapeet.exam.dto.UpdateTestRequest;
import com.vidyapeet.exam.repository.BatchTestRepository;
import com.vidyapeet.exam.repository.MockTestRepository;
import com.vidyapeet.exam.repository.QuestionRepository;
import com.vidyapeet.exam.repository.TestQuestionReferenceRepository;
import com.vidyapeet.library.repository.LibraryFolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * INSTITUTE_ADMIN management of tests and their question bank, including bulk
 * import from Excel. Total marks are always kept in sync with the questions.
 */
@Service
public class ExamService {

    private final MockTestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestQuestionReferenceRepository referenceRepository;
    private final BatchRepository batchRepository;
    private final LibraryFolderRepository folderRepository;
    private final BatchTestRepository batchTestRepository;
    private final TestAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final QuestionExcelImporter excelImporter;
    private final QuestionBankService questionBankService;

    public ExamService(
            MockTestRepository testRepository,
            QuestionRepository questionRepository,
            TestQuestionReferenceRepository referenceRepository,
            BatchRepository batchRepository,
            LibraryFolderRepository folderRepository,
            BatchTestRepository batchTestRepository,
            TestAttemptRepository attemptRepository,
            AttemptAnswerRepository answerRepository,
            QuestionExcelImporter excelImporter,
            QuestionBankService questionBankService) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.referenceRepository = referenceRepository;
        this.batchRepository = batchRepository;
        this.folderRepository = folderRepository;
        this.batchTestRepository = batchTestRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.excelImporter = excelImporter;
        this.questionBankService = questionBankService;
    }

    @Transactional
    public TestResponse createTest(CreateTestRequest request) {
        boolean hasBatch = request.batchId() != null;
        boolean hasFolder = request.folderId() != null;
        if (hasBatch == hasFolder) {
            throw Exceptions.badRequest("Provide exactly one of batchId (batch test) or folderId (library test).");
        }

        MockTest test = new MockTest();
        if (hasBatch) {
            requireBatch(request.batchId());
            test.setBatchId(request.batchId());
        } else {
            requireFolder(request.folderId());
            test.setFolderId(request.folderId());
        }
        test.setTitle(request.title());
        test.setDurationMinutes(request.durationMinutes());
        test.setTotalMarks(0);
        test.setPublished(false);
        test.setTestType(request.testType() == null ? TestType.EXAM : request.testType());
        test.setNegativeMarking(request.negativeMarking());
        test.setNegativeMarkPerWrong(request.negativeMarkPerWrong() == null ? 0 : request.negativeMarkPerWrong());
        return TestResponse.from(testRepository.save(test), 0);
    }

    /** Tests visible in a batch: created directly in it plus library tests shared to it. */
    @Transactional(readOnly = true)
    public List<TestResponse> listTests(Long batchId) {
        requireBatch(batchId);
        Map<Long, MockTest> byId = new LinkedHashMap<>();
        for (MockTest t : testRepository.findByBatchIdOrderByCreatedAtDesc(batchId)) {
            byId.put(t.getId(), t);
        }
        List<Long> assignedIds = batchTestRepository.findByBatchId(batchId).stream()
                .map(com.vidyapeet.exam.BatchTest::getTestId).toList();
        if (!assignedIds.isEmpty()) {
            for (MockTest t : testRepository.findAllById(assignedIds)) {
                byId.putIfAbsent(t.getId(), t);
            }
        }
        return byId.values().stream()
                .map(t -> TestResponse.from(t, referenceRepository.countByTestId(t.getId())))
                .toList();
    }

    /** Library tests inside a folder. */
    @Transactional(readOnly = true)
    public List<TestResponse> listLibraryTests(Long folderId) {
        return testRepository.findByFolderIdOrderByCreatedAtDesc(folderId).stream()
                .map(t -> TestResponse.from(t, referenceRepository.countByTestId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDetailResponse getTest(Long id) {
        MockTest test = requireTest(id);
        List<Question> questions = referenceRepository.findResolvedQuestions(id);
        // Map each referenced bank question to the section it is grouped under within this
        // test (null = ungrouped); a bank question is referenced at most once per test.
        Map<Long, Long> sectionByQuestionId = new HashMap<>();
        for (TestQuestionReference reference
                : referenceRepository.findByTestIdOrderBySectionPositionAscPositionAsc(id)) {
            sectionByQuestionId.put(reference.getBankQuestionId(), reference.getSectionId());
        }
        return TestDetailResponse.from(
                test, questions, sectionByQuestionId, questionBankService.listSections(id));
    }

    @Transactional
    public TestResponse updateTest(Long id, UpdateTestRequest request) {
        MockTest test = requireTest(id);
        test.setTitle(request.title());
        test.setDurationMinutes(request.durationMinutes());
        if (request.testType() != null) {
            test.setTestType(request.testType());
        }
        test.setNegativeMarking(request.negativeMarking());
        test.setNegativeMarkPerWrong(request.negativeMarkPerWrong() == null ? 0 : request.negativeMarkPerWrong());
        long questionCount = referenceRepository.countByTestId(id);
        if (request.published() && questionCount == 0) {
            throw Exceptions.badRequest("A test must have at least one question before it can be published.");
        }
        test.setPublished(request.published());
        return TestResponse.from(testRepository.save(test), questionCount);
    }

    @Transactional
    public void deleteTest(Long id) {
        MockTest test = requireTest(id);
        // Remove attempts + their answers, batch assignments, then questions.
        List<Long> attemptIds = attemptRepository.findByTestId(id).stream()
                .map(TestAttempt::getId).toList();
        if (!attemptIds.isEmpty()) {
            answerRepository.deleteByAttemptIdIn(attemptIds);
        }
        attemptRepository.deleteByTestId(id);
        batchTestRepository.deleteByTestId(id);
        // Reference-aware cleanup: drop this test's references only; the bank questions
        // they point to remain in the institute Question Bank for reuse by other tests.
        referenceRepository.deleteByTestId(id);
        testRepository.delete(test);
    }

    @Transactional
    public QuestionResponse addQuestion(Long testId, QuestionRequest request) {
        requireTest(testId);
        // The per-test editor flow attaches to the shared bank: create the bank
        // question, then reference it from this test (no per-test content copy).
        QuestionResponse created = questionBankService.createBankQuestion(request);
        questionBankService.attachReference(testId, created.id());
        return created;
    }

    @Transactional
    public QuestionResponse updateQuestion(Long testId, Long questionId, QuestionRequest request) {
        requireTest(testId);
        requireQuestion(testId, questionId);
        // Editing the bank question in place reflects in every referencing test (Req 6.4).
        return questionBankService.updateBankQuestion(questionId, request);
    }

    @Transactional
    public void deleteQuestion(Long testId, Long questionId) {
        requireTest(testId);
        requireQuestion(testId, questionId);
        // Detach the reference from this test; only remove the bank question itself when
        // no other test still references it (retain shared bank questions — Req 6.9).
        questionBankService.detachReference(testId, questionId);
        if (referenceRepository.findByBankQuestionId(questionId).isEmpty()) {
            questionRepository.deleteById(questionId);
        }
    }

    @Transactional
    public int importQuestions(Long testId, MultipartFile file) {
        requireTest(testId);
        List<QuestionRequest> parsed = excelImporter.parse(file);
        for (QuestionRequest request : parsed) {
            QuestionResponse created = questionBankService.createBankQuestion(request);
            questionBankService.attachReference(testId, created.id());
        }
        return parsed.size();
    }

    private void requireBatch(Long batchId) {
        if (batchRepository.findById(batchId).isEmpty()) {
            throw Exceptions.notFound("No batch found with id " + batchId + ".");
        }
    }

    private void requireFolder(Long folderId) {
        if (folderRepository.findById(folderId).isEmpty()) {
            throw Exceptions.notFound("No library folder found with id " + folderId + ".");
        }
    }

    private MockTest requireTest(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No test found with id " + id + "."));
    }

    private Question requireQuestion(Long testId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> Exceptions.notFound("No question found with id " + questionId + "."));
        if (!referenceRepository.existsByTestIdAndBankQuestionId(testId, questionId)) {
            throw Exceptions.notFound("No question found with id " + questionId + " for this test.");
        }
        return question;
    }
}
