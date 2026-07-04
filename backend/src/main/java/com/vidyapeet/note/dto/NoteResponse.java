package com.vidyapeet.note.dto;

import com.vidyapeet.note.Note;

import java.time.Instant;

public record NoteResponse(
        Long id,
        Long batchId,
        String subject,
        String title,
        Long fileSize,
        String downloadUrl,
        Instant uploadedAt
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getBatchId(),
                note.getSubject(),
                note.getTitle(),
                note.getFileSize(),
                "/api/notes/" + note.getId() + "/download",
                note.getCreatedAt());
    }
}
