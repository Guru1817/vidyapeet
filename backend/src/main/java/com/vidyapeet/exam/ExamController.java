package com.vidyapeet.exam;

import com.vidyapeet.exam.dto.CreateTestRequest;
import com.vidyapeet.exam.dto.QuestionRequest;
import com.vidyapeet.exam.dto.QuestionResponse;
import com.vidyapeet.exam.dto.TestDetailResponse;
import com.vidyapeet.exam.dto.TestResponse;
import com.vidyapeet.exam.dto.UpdateTestRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * INSTITUTE_ADMIN management of tests and their question bank.
 */
@RestController
@RequestMapping("/api/tests")
@PreAuthorize("hasRole('INSTITUTE_ADMIN')")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    public ResponseEntity<TestResponse> create(@Valid @RequestBody CreateTestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.createTest(request));
    }

    @GetMapping
    public List<TestResponse> list(@RequestParam Long batchId) {
        return examService.listTests(batchId);
    }

    @GetMapping("/{id}")
    public TestDetailResponse get(@PathVariable Long id) {
        return examService.getTest(id);
    }

    @PutMapping("/{id}")
    public TestResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTestRequest request) {
        return examService.updateTest(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(
            @PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.addQuestion(id, request));
    }

    @PutMapping("/{id}/questions/{questionId}")
    public QuestionResponse updateQuestion(
            @PathVariable Long id, @PathVariable Long questionId, @Valid @RequestBody QuestionRequest request) {
        return examService.updateQuestion(id, questionId, request);
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        examService.deleteQuestion(id, questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions/import")
    public ResponseEntity<Map<String, Object>> importQuestions(
            @PathVariable Long id, @RequestParam MultipartFile file) {
        int imported = examService.importQuestions(id, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("imported", imported));
    }
}
