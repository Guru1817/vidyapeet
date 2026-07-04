package com.vidyapeet.library;

import com.vidyapeet.library.dto.FolderDetailResponse;
import com.vidyapeet.library.dto.FolderRequest;
import com.vidyapeet.library.dto.FolderResponse;
import com.vidyapeet.library.dto.LibraryFileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping("/folders")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public ResponseEntity<FolderResponse> createFolder(@Valid @RequestBody FolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.createFolder(request));
    }

    @GetMapping("/folders")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public List<FolderResponse> listFolders() {
        return libraryService.listFolders();
    }

    @GetMapping("/folders/{id}")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public FolderDetailResponse folder(@PathVariable Long id) {
        return libraryService.getFolderDetail(id);
    }

    @PutMapping("/folders/{id}")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public FolderResponse updateFolder(@PathVariable Long id, @Valid @RequestBody FolderRequest request) {
        return libraryService.updateFolder(id, request);
    }

    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        libraryService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/folders/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public ResponseEntity<LibraryFileResponse> uploadFile(
            @PathVariable Long id,
            @RequestParam @NotBlank String subject,
            @RequestParam @NotBlank String title,
            @RequestParam MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(libraryService.uploadFile(id, subject, title, file));
    }

    @DeleteMapping("/files/{id}")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        libraryService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        LibraryService.DownloadableFile file = libraryService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .body(file.resource());
    }
}
