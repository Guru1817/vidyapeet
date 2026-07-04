package com.vidyapeet.user;

import com.vidyapeet.user.dto.CreateStudentRequest;
import com.vidyapeet.user.dto.StudentResponse;
import com.vidyapeet.user.dto.UpdateStudentRequest;
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
@RequestMapping("/api/students")
@PreAuthorize("hasRole('INSTITUTE_ADMIN')")
public class StudentAdminController {

    private final StudentAdminService studentAdminService;

    public StudentAdminController(StudentAdminService studentAdminService) {
        this.studentAdminService = studentAdminService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentAdminService.create(request));
    }

    @GetMapping
    public List<StudentResponse> list() {
        return studentAdminService.list();
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
