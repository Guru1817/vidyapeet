package com.vidyapeet.note;

import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.batch.repository.BatchStudentRepository;
import com.vidyapeet.common.Role;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.library.BatchLibraryFile;
import com.vidyapeet.library.LibraryFile;
import com.vidyapeet.library.repository.BatchLibraryFileRepository;
import com.vidyapeet.library.repository.LibraryFileRepository;
import com.vidyapeet.note.dto.NoteResponse;
import com.vidyapeet.note.repository.NoteRepository;
import com.vidyapeet.security.SecurityUtils;
import com.vidyapeet.security.UserPrincipal;
import com.vidyapeet.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final BatchRepository batchRepository;
    private final BatchStudentRepository batchStudentRepository;
    private final LibraryFileRepository libraryFileRepository;
    private final BatchLibraryFileRepository batchLibraryFileRepository;
    private final StorageService storageService;

    public NoteService(
            NoteRepository noteRepository,
            BatchRepository batchRepository,
            BatchStudentRepository batchStudentRepository,
            LibraryFileRepository libraryFileRepository,
            BatchLibraryFileRepository batchLibraryFileRepository,
            StorageService storageService) {
        this.noteRepository = noteRepository;
        this.batchRepository = batchRepository;
        this.batchStudentRepository = batchStudentRepository;
        this.libraryFileRepository = libraryFileRepository;
        this.batchLibraryFileRepository = batchLibraryFileRepository;
        this.storageService = storageService;
    }

    /** A streamable file plus a friendly download filename. */
    public record DownloadableFile(Resource resource, String filename) {
    }

    @Transactional
    public NoteResponse upload(Long batchId, String subject, String title, MultipartFile file) {
        requireBatch(batchId);
        String key = storageService.store(file);

        Note note = new Note();
        note.setBatchId(batchId);
        note.setSubject(subject);
        note.setTitle(title);
        note.setFileUrl(key);
        note.setFileSize(file.getSize());
        note.setUploadedBy(SecurityUtils.currentUserId());
        return NoteResponse.from(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listForBatch(Long batchId) {
        requireBatch(batchId);
        return noteRepository.findByBatchIdOrderByCreatedAtDesc(batchId).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listForCurrentStudent() {
        List<Long> batchIds = enrolledBatchIds(SecurityUtils.currentUserId());
        if (batchIds.isEmpty()) {
            return List.of();
        }
        List<NoteResponse> materials = new ArrayList<>(
                noteRepository.findByBatchIdInOrderByCreatedAtDesc(batchIds).stream()
                        .map(NoteResponse::from)
                        .toList());

        // Library files shared with the student's batches.
        List<Long> fileIds = batchLibraryFileRepository.findByBatchIdIn(batchIds).stream()
                .map(BatchLibraryFile::getLibraryFileId)
                .distinct()
                .toList();
        if (!fileIds.isEmpty()) {
            for (LibraryFile f : libraryFileRepository.findAllById(fileIds)) {
                materials.add(new NoteResponse(
                        f.getId(), null, f.getSubject(), f.getTitle(), f.getFileSize(),
                        "/api/library/files/" + f.getId() + "/download", f.getCreatedAt()));
            }
        }
        return materials;
    }

    @Transactional
    public void delete(Long id) {
        Note note = requireNote(id);
        noteRepository.delete(note);
        storageService.delete(note.getFileUrl());
    }

    @Transactional(readOnly = true)
    public DownloadableFile download(Long id) {
        Note note = requireNote(id);
        authorizeDownload(note);
        Resource resource = storageService.loadAsResource(note.getFileUrl());
        String safeTitle = note.getTitle().replaceAll("[^a-zA-Z0-9-_ ]", "_").trim();
        return new DownloadableFile(resource, safeTitle + ".pdf");
    }

    private void authorizeDownload(Note note) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == Role.INSTITUTE_ADMIN) {
            // Tenant filter already guarantees the note is in the admin's institute.
            return;
        }
        if (principal.getRole() == Role.STUDENT) {
            if (!batchStudentRepository.existsByBatchIdAndStudentId(note.getBatchId(), principal.getUserId())) {
                throw Exceptions.forbidden("You are not enrolled in the batch for this note.");
            }
            return;
        }
        throw Exceptions.forbidden("You cannot access this note.");
    }

    private List<Long> enrolledBatchIds(Long studentId) {
        return batchStudentRepository.findByStudentId(studentId).stream()
                .map(bs -> bs.getBatchId())
                .toList();
    }

    private void requireBatch(Long batchId) {
        if (batchRepository.findById(batchId).isEmpty()) {
            throw Exceptions.notFound("No batch found with id " + batchId + ".");
        }
    }

    private Note requireNote(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("No note found with id " + id + "."));
    }
}
