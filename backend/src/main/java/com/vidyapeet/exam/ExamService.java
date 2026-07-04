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
import com.vidyapeet.library.repository.LibraryFolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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
    private final BatchRepository batchRepository;
    private final LibraryFolderRepository folderRepository;
    private final BatchTestRepository batchTestRepository;
    private final TestAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final QuestionExcelImporter excelImporter;

    public ExamService(
            MockTestRepository testRepository,
            QuestionRepository questionRepository,
            BatchRepository batchRepository,
            LibraryFolderRepository folderRepository,
            BatchTestRepository batchTestRepository,
            TestAttemptRepository attemptRepository,
            AttemptAnswerRepository answerRepository,
            QuestionExcelImporter excelImporter) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.batchRepository = batchRepository;
        this.folderRepository = folderRepository;
        this.batchTestRepository = batchTestRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.excelImporter = excelImporter;
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
                .map(t -> TestResponse.from(t, questionRepository.countByTestId(t.getId())))
                .toList();
    }

    /** Library tests inside a folder. */
    @Transactional(readOnly = true)
    public List<TestResponse> listLibraryTests(Long folderId) {
        return testRepository.findByFolderIdOrderByCreatedAtDesc(folderId).stream()
                .map(t -> TestResponse.from(t, questionRepository.countByTestId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDetailResponse getTest(Long id) {
        MockTest test = requireTest(id);
        return TestDetailResponse.from(test, questionRepository.findByTestId(id));
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
        long questionCount = questionRepository.countByTestId(id);
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
        questionRepository.deleteByTestId(id);
        testRepository.delete(test);
    }

    @Transactional
    public QuestionResponse addQuestion(Long testId, QuestionRequest request) {
        requireTest(testId);
        Question question = applyRequest(new Question(), testId, request);
        question = questionRepository.save(question);
        recomputeTotalMarks(testId);
        return QuestionResponse.from(question);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long testId, Long questionId, QuestionRequest request) {
        requireTest(testId);
        Question question = requireQuestion(testId, questionId);
        applyRequest(question, testId, request);
        question = questionRepository.save(question);
        recomputeTotalMarks(testId);
        return QuestionResponse.from(question);
    }

    @Transactional
    public void deleteQuestion(Long testId, Long questionId) {
        requireTest(testId);
        Question question = requireQuestion(testId, questionId);
        questionRepository.delete(question);
        recomputeTotalMarks(testId);
    }

    @Transactional
    public int importQuestions(Long testId, MultipartFile file) {
        requireTest(testId);
        List<QuestionRequest> parsed = excelImporter.parse(file);
        for (QuestionRequest request : parsed) {
            questionRepository.save(applyRequest(new Question(), testId, request));
        }
        recomputeTotalMarks(testId);
        return parsed.size();
    }

    private Question applyRequest(Question question, Long testId, QuestionRequest request) {
        question.setTestId(testId);
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

    private void recomputeTotalMarks(Long testId) {
        int total = questionRepository.findByTestId(testId).stream()
                .mapToInt(Question::getMarks)
                .sum();
        MockTest test = requireTest(testId);
        test.setTotalMarks(total);
        testRepository.save(test);
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
        if (!question.getTestId().equals(testId)) {
            throw Exceptions.notFound("No question found with id " + questionId + " for this test.");
        }
        return question;
    }
}
