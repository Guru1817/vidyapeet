package com.vidyapeet.exam.repository;

import com.vidyapeet.exam.Question;
import com.vidyapeet.exam.TestQuestionReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * References attaching bank {@link Question}s to tests. Inherits tenant-safe
 * {@code findById}/{@code existsById} via {@code TenantAwareJpaRepository}; the
 * {@code @Filter} on {@link TestQuestionReference} and {@link Question} scopes every
 * query to the current institute.
 */
public interface TestQuestionReferenceRepository extends JpaRepository<TestQuestionReference, Long> {

    /**
     * References for a test ordered by the owning section's position then the reference
     * position within that section.
     *
     * <p>Joins each reference to its {@link com.vidyapeet.exam.TestSection} (an ad-hoc join
     * on {@code section_id}, which is nullable). Ungrouped references ({@code section_id}
     * null) sort first via {@code NULLS FIRST}; when a test defines no sections every
     * reference is ungrouped and the result is a single list ordered by reference position.
     */
    @Query("SELECT r FROM TestQuestionReference r "
            + "LEFT JOIN TestSection s ON s.id = r.sectionId "
            + "WHERE r.testId = :testId "
            + "ORDER BY s.position ASC NULLS FIRST, r.position ASC")
    List<TestQuestionReference> findByTestIdOrderBySectionPositionAscPositionAsc(@Param("testId") Long testId);

    List<TestQuestionReference> findByBankQuestionId(Long bankQuestionId);

    java.util.Optional<TestQuestionReference> findByTestIdAndBankQuestionId(Long testId, Long bankQuestionId);

    void deleteByTestIdAndBankQuestionId(Long testId, Long bankQuestionId);

    void deleteByTestId(Long testId);

    void deleteByInstituteId(Long instituteId);

    long countByTestId(Long testId);

    boolean existsByTestIdAndBankQuestionId(Long testId, Long bankQuestionId);

    /**
     * Resolved bank questions for a test, ordered by the owning section's position then
     * reference position (see {@link #findByTestIdOrderBySectionPositionAscPositionAsc}).
     * Returns the {@link Question} bank entities so grading and rendering operate on the
     * live, shared definitions rather than per-test copies. Ungrouped references sort first
     * via {@code NULLS FIRST}, so an unsectioned test yields a single list ordered by
     * reference position.
     */
    @Query("SELECT q FROM Question q, TestQuestionReference r "
            + "LEFT JOIN TestSection s ON s.id = r.sectionId "
            + "WHERE r.bankQuestionId = q.id AND r.testId = :testId "
            + "ORDER BY s.position ASC NULLS FIRST, r.position ASC")
    List<Question> findResolvedQuestions(@Param("testId") Long testId);
}
