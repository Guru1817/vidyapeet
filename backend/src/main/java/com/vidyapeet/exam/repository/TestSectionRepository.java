package com.vidyapeet.exam.repository;

import com.vidyapeet.exam.TestSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Test sections owned by an institute. Inherits tenant-safe {@code findById}/{@code existsById}
 * via {@code TenantAwareJpaRepository}; the {@code @Filter} on {@link TestSection} scopes every
 * query to the current institute.
 */
public interface TestSectionRepository extends JpaRepository<TestSection, Long> {

    /** Sections of a test in display order. */
    List<TestSection> findByTestIdOrderByPositionAsc(Long testId);
}
