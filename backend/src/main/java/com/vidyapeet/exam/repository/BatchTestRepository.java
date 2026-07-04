package com.vidyapeet.exam.repository;

import com.vidyapeet.exam.BatchTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchTestRepository extends JpaRepository<BatchTest, Long> {

    List<BatchTest> findByBatchId(Long batchId);

    List<BatchTest> findByBatchIdIn(List<Long> batchIds);

    List<BatchTest> findByTestId(Long testId);

    boolean existsByBatchIdAndTestId(Long batchId, Long testId);

    Optional<BatchTest> findByBatchIdAndTestId(Long batchId, Long testId);

    void deleteByTestId(Long testId);

    void deleteByInstituteId(Long instituteId);
}
