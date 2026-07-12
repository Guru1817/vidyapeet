package com.vidyapeet.exam;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Question image lifecycle, mirroring {@link com.vidyapeet.library.LibraryController}'s
 * private-bucket streaming pattern. Upload/delete are INSTITUTE_ADMIN only; download is
 * available to any authenticated user (e.g. students taking a test). Tenant isolation is
 * enforced by the {@code @Filter} on {@link Question}.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionImageController {

    private final QuestionImageService questionImageService;

    public QuestionImageController(QuestionImageService questionImageService) {
        this.questionImageService = questionImageService;
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id, @RequestParam MultipartFile file) {
        String key = questionImageService.uploadImage(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("imageKey", key));
    }

    @GetMapping("/{id}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        QuestionImageService.StreamableImage image = questionImageService.getImage(id);
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .body(image.resource());
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        questionImageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
