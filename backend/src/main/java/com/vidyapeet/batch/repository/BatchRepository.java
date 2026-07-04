package com.vidyapeet.batch.repository;

import com.vidyapeet.batch.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findAllByOrderByNameAsc();

    void deleteByInstituteId(Long instituteId);
}
