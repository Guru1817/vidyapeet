package com.vidyapeet.exam;

import com.vidyapeet.exam.dto.AssignReferenceSectionRequest;
import com.vidyapeet.exam.dto.CreateReferenceRequest;
import com.vidyapeet.exam.dto.CreateTestRequest;
import com.vidyapeet.exam.dto.QuestionRequest;
import com.vidyapeet.exam.dto.QuestionResponse;
import com.vidyapeet.exam.dto.SectionRequest;
import com.vidyapeet.exam.dto.SectionResponse;
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
    private final QuestionBankService questionBankService;

    public ExamController(ExamService examService, QuestionBankService questionBankService) {
        this.examService = examService;
        this.questionBankService = questionBankService;
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

    /** Attach a bank question to this test by reference (no content copy). */
    @PostMapping("/{id}/references")
    public ResponseEntity<Void> createReference(
            @PathVariable Long id, @Valid @RequestBody CreateReferenceRequest request) {
        questionBankService.attachReference(id, request.bankQuestionId(), request.sectionId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Detach a bank question from this test, retaining it in the bank (Req 6.9). */
    @DeleteMapping("/{id}/references")
    public ResponseEntity<Void> deleteReference(
            @PathVariable Long id, @RequestParam Long bankQuestionId) {
        questionBankService.detachReference(id, bankQuestionId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------
    // Test sections (labeled groupings under one overall timer — Req 7)
    // -----------------------------------------------------------------

    /** Sections of a test in display order (Req 7.1). */
    @GetMapping("/{id}/sections")
    public List<SectionResponse> listSections(@PathVariable Long id) {
        return questionBankService.listSections(id);
    }

    /** Create a labeled section for a test (Req 7.1). */
    @PostMapping("/{id}/sections")
    public ResponseEntity<SectionResponse> createSection(
            @PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionBankService.createSection(id, request));
    }

    /** Rename and/or reorder a section (Req 7.1). */
    @PutMapping("/{id}/sections/{sectionId}")
    public SectionResponse updateSection(
            @PathVariable Long id, @PathVariable Long sectionId, @Valid @RequestBody SectionRequest request) {
        return questionBankService.updateSection(id, sectionId, request);
    }

    /** Delete a section; its references are moved back to the ungrouped list (Req 7.8). */
    @DeleteMapping("/{id}/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id, @PathVariable Long sectionId) {
        questionBankService.deleteSection(id, sectionId);
        return ResponseEntity.noContent().build();
    }

    /** Group (or ungroup) a reference under a section (Req 7.2). */
    @PutMapping("/{id}/references/section")
    public ResponseEntity<Void> assignReferenceToSection(
            @PathVariable Long id, @Valid @RequestBody AssignReferenceSectionRequest request) {
        questionBankService.assignReferenceToSection(id, request.bankQuestionId(), request.sectionId());
        return ResponseEntity.noContent().build();
    }
}
