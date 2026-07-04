package com.vidyapeet.exam;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * A timed MCQ test assigned to a batch. Named {@code MockTest} to avoid clashing
 * with testing conventions; mapped to the {@code tests} table.
 */
@Entity
@Table(name = "tests")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class MockTest extends TenantBaseEntity {

    /** Set for batch-native tests; null for library tests (see folderId). */
    @Column(name = "batch_id")
    private Long batchId;

    /** Set for library tests; null for batch-native tests. */
    @Column(name = "folder_id")
    private Long folderId;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /** Sum of question marks; kept in sync when questions change. */
    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks = 0;

    @Column(name = "is_published", nullable = false)
    private boolean published = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 16)
    private TestType testType = TestType.EXAM;

    /** When true, wrong (answered) questions deduct {@link #negativeMarkPerWrong}. */
    @Column(name = "negative_marking", nullable = false)
    private boolean negativeMarking = false;

    /** Marks deducted per wrong answer when negative marking is on (e.g. 0.25). */
    @Column(name = "negative_mark_per_wrong", nullable = false)
    private double negativeMarkPerWrong = 0;
}
