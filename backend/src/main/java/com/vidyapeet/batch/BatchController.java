package com.vidyapeet.batch;

import com.vidyapeet.batch.dto.BatchResponse;
import com.vidyapeet.batch.dto.CreateBatchRequest;
import com.vidyapeet.batch.dto.EnrollStudentRequest;
import com.vidyapeet.batch.dto.UpdateBatchRequest;
import com.vidyapeet.library.LibraryService;
import com.vidyapeet.library.dto.LibraryFileResponse;
import com.vidyapeet.user.dto.StudentResponse;
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

@RestController
@RequestMapping("/api/batches")
@PreAuthorize("hasRole('INSTITUTE_ADMIN')")
public class BatchController {

    private final BatchService batchService;
    private final LibraryService libraryService;

    public BatchController(BatchService batchService, LibraryService libraryService) {
        this.batchService = batchService;
        this.libraryService = libraryService;
    }

    @PostMapping
    public ResponseEntity<BatchResponse> create(@Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.create(request));
    }

    @GetMapping
    public List<BatchResponse> list() {
        return batchService.list();
    }

    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable Long id) {
        return batchService.get(id);
    }

    @PutMapping("/{id}")
    public BatchResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBatchRequest request) {
        return batchService.update(id, request);
    }

    @PostMapping("/{id}/students")
    public ResponseEntity<Void> enroll(@PathVariable Long id, @Valid @RequestBody EnrollStudentRequest request) {
        batchService.enroll(id, request.studentId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<Void> unenroll(@PathVariable Long id, @PathVariable Long studentId) {
        batchService.unenroll(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    public List<StudentResponse> listStudents(@PathVariable Long id) {
        return batchService.listStudents(id);
    }

    // --- library assignments (shared content) ---

    @GetMapping("/{id}/library-files")
    public List<LibraryFileResponse> assignedFiles(@PathVariable Long id) {
        return libraryService.listBatchFiles(id);
    }

    @PostMapping("/{id}/library-files/{fileId}")
    public ResponseEntity<Void> assignFile(@PathVariable Long id, @PathVariable Long fileId) {
        libraryService.assignFileToBatch(id, fileId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/library-files/{fileId}")
    public ResponseEntity<Void> unassignFile(@PathVariable Long id, @PathVariable Long fileId) {
        libraryService.unassignFileFromBatch(id, fileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/library-tests/{testId}")
    public ResponseEntity<Void> assignTest(@PathVariable Long id, @PathVariable Long testId) {
        libraryService.assignTestToBatch(id, testId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/library-tests/{testId}")
    public ResponseEntity<Void> unassignTest(@PathVariable Long id, @PathVariable Long testId) {
        libraryService.unassignTestFromBatch(id, testId);
        return ResponseEntity.noContent().build();
    }
}
