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
 * Assignment of a library test to a batch (shared, not copied). Students of the
 * batch can take the test; results and leaderboard are shared across all
 * assigned batches.
 */
@Entity
@Table(
        name = "batch_tests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_batch_test",
                columnNames = {"batch_id", "test_id"}
        )
)
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class BatchTest extends TenantBaseEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "test_id", nullable = false)
    private Long testId;
}
