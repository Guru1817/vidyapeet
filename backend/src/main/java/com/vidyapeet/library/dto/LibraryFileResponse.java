package com.vidyapeet.library.dto;

import com.vidyapeet.library.LibraryFile;

import java.time.Instant;

public record LibraryFileResponse(
        Long id,
        Long folderId,
        String subject,
        String title,
        Long fileSize,
        String downloadUrl,
        Instant uploadedAt
) {
    public static LibraryFileResponse from(LibraryFile f) {
        return new LibraryFileResponse(
                f.getId(), f.getFolderId(), f.getSubject(), f.getTitle(), f.getFileSize(),
                "/api/library/files/" + f.getId() + "/download", f.getCreatedAt());
    }
}
