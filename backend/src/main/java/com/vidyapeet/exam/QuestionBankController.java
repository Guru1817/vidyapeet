package com.vidyapeet.exam;

import com.vidyapeet.exam.dto.QuestionRequest;
import com.vidyapeet.exam.dto.QuestionResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * INSTITUTE_ADMIN CRUD over the per-institute Question Bank. Bank edits happen in
 * place (no content copy), so a change is reflected in every test that references the
 * question. Tenant isolation is enforced by the {@code @Filter} on {@link Question}.
 */
@RestController
@RequestMapping("/api/bank/questions")
@PreAuthorize("hasRole('INSTITUTE_ADMIN')")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public List<QuestionResponse> list() {
        return questionBankService.listBankQuestions();
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable Long id) {
        return questionBankService.getBankQuestion(id);
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createBankQuestion(request));
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return questionBankService.updateBankQuestion(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionBankService.deleteBankQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
