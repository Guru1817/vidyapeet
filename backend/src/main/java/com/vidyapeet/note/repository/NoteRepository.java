package com.vidyapeet.note.repository;

import com.vidyapeet.note.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    List<Note> findByBatchIdInOrderByCreatedAtDesc(List<Long> batchIds);

    void deleteByInstituteId(Long instituteId);
}
