package com.vidyapeet.exam;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Reuse-by-reference link that attaches a bank {@link Question} to a {@link MockTest}.
 * A single bank question may be referenced by many tests; editing the bank question
 * therefore reflects in every referencing test. The optional {@code sectionId} groups
 * the reference under a {@code TestSection} (added in a later migration; kept as a plain
 * nullable column for now) and {@code position} orders references within the test.
 */
@Entity
@Table(
        name = "test_question_references",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_test_question_ref",
                columnNames = {"test_id", "bank_question_id"}
        )
)
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class TestQuestionReference extends TenantBaseEntity {

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "bank_question_id", nullable = false)
    private Long bankQuestionId;

    /** Nullable grouping into a test section; {@code null} means ungrouped. */
    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "position", nullable = false)
    private Integer position = 0;
}
