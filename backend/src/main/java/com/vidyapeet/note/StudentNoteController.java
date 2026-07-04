package com.vidyapeet.note;

import com.vidyapeet.note.dto.NoteResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notes visible to the logged-in student: only those for batches they are
 * enrolled in. Downloads use the shared {@code /api/notes/{id}/download} endpoint.
 */
@RestController
@RequestMapping("/api/student/notes")
@PreAuthorize("hasRole('STUDENT')")
public class StudentNoteController {

    private final NoteService noteService;

    public StudentNoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<NoteResponse> myNotes() {
        return noteService.listForCurrentStudent();
    }
}
