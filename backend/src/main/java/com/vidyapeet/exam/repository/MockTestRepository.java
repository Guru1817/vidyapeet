package com.vidyapeet.exam.repository;

import com.vidyapeet.exam.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockTestRepository extends JpaRepository<MockTest, Long> {

    List<MockTest> findByBatchIdOrderByCreatedAtDesc(Long batchId);

    List<MockTest> findByFolderIdOrderByCreatedAtDesc(Long folderId);

    long countByFolderId(Long folderId);

    List<MockTest> findByIdInAndPublishedTrueOrderByCreatedAtDesc(List<Long> ids);

    List<MockTest> findByBatchIdInAndPublishedTrueOrderByCreatedAtDesc(List<Long> batchIds);

    void deleteByInstituteId(Long instituteId);
}
