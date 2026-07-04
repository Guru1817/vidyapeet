package com.vidyapeet.library.repository;

import com.vidyapeet.library.LibraryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryFileRepository extends JpaRepository<LibraryFile, Long> {

    List<LibraryFile> findByFolderIdOrderByCreatedAtDesc(Long folderId);

    long countByFolderId(Long folderId);

    void deleteByFolderId(Long folderId);

    void deleteByInstituteId(Long instituteId);
}
