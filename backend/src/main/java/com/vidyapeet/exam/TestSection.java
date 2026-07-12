package com.vidyapeet.exam;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * A labeled, ordered grouping of questions within a single {@link MockTest}. Sections are
 * organizational only: they carry no timing of their own, the whole attempt runs under the
 * test's single overall timer. A {@link TestQuestionReference} points at its section via
 * {@code section_id} ({@code null} = ungrouped), and {@code position} orders the sections
 * within the test.
 */
@Entity
@Table(name = "test_sections")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class TestSection extends TenantBaseEntity {

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "position", nullable = false)
    private Integer position = 0;
}
