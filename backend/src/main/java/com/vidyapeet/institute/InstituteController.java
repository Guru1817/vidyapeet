package com.vidyapeet.institute;

import com.vidyapeet.institute.dto.CreateInstituteRequest;
import com.vidyapeet.institute.dto.InstituteResponse;
import com.vidyapeet.institute.dto.UpdateInstituteRequest;
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
 * Platform-owner endpoints for managing coaching centers. Restricted to
 * SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/institutes")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InstituteController {

    private final InstituteService instituteService;

    public InstituteController(InstituteService instituteService) {
        this.instituteService = instituteService;
    }

    @PostMapping
    public ResponseEntity<InstituteResponse> create(@Valid @RequestBody CreateInstituteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instituteService.create(request));
    }

    @GetMapping
    public List<InstituteResponse> list() {
        return instituteService.list();
    }

    @GetMapping("/{id}")
    public InstituteResponse get(@PathVariable Long id) {
        return instituteService.get(id);
    }

    @PutMapping("/{id}")
    public InstituteResponse update(@PathVariable Long id, @Valid @RequestBody UpdateInstituteRequest request) {
        return instituteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        instituteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
