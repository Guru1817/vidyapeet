package com.vidyapeet.batch;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Enrollment of a student into a batch. Modelled as a tenant-scoped entity so the
 * same isolation guarantees apply to enrollment data.
 */
@Entity
@Table(
        name = "batch_students",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_batch_student",
                columnNames = {"batch_id", "student_id"}
        )
)
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class BatchStudent extends TenantBaseEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;
}
