package com.vidyapeet.attempt;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;

/**
 * A student's attempt at a test. EXAM tests allow one graded attempt; PRACTICE
 * tests allow many, so this table no longer enforces a single attempt per
 * student — that rule is applied per test type in the service layer.
 */
@Entity
@Table(name = "test_attempts")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class TestAttempt extends TenantBaseEntity {

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** May be fractional/negative when negative marking is enabled. */
    @Column(nullable = false)
    private Double score = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
