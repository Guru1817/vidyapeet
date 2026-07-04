package com.vidyapeet.library.repository;

import com.vidyapeet.library.BatchLibraryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchLibraryFileRepository extends JpaRepository<BatchLibraryFile, Long> {

    List<BatchLibraryFile> findByBatchId(Long batchId);

    List<BatchLibraryFile> findByBatchIdIn(List<Long> batchIds);

    boolean existsByBatchIdAndLibraryFileId(Long batchId, Long libraryFileId);

    Optional<BatchLibraryFile> findByBatchIdAndLibraryFileId(Long batchId, Long libraryFileId);

    void deleteByLibraryFileId(Long libraryFileId);

    void deleteByInstituteId(Long instituteId);
}
