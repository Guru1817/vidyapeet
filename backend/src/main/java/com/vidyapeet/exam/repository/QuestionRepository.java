package com.vidyapeet.exam.repository;

import com.vidyapeet.exam.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * The per-institute Question Bank. Questions are no longer owned by a single test;
 * test membership is resolved through {@code TestQuestionReferenceRepository}. Inherits
 * tenant-safe {@code findById}/{@code existsById} via {@code TenantAwareJpaRepository}.
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByInstituteId(Long instituteId);

    void deleteByInstituteId(Long instituteId);
}
