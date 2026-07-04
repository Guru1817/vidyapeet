package com.vidyapeet.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

/**
 * Base class for every tenant-scoped entity. Carries the {@code institute_id}
 * discriminator column and wires the {@link TenantEntityListener} that stamps it
 * automatically on insert.
 *
 * <p>The {@link FilterDef} declared here is enabled per-session by
 * {@link TenantFilterAspect}; concrete entities apply it via {@code @Filter}.
 */
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@FilterDef(
        name = TenantBaseEntity.TENANT_FILTER,
        parameters = @ParamDef(name = TenantBaseEntity.TENANT_PARAM, type = Long.class)
)
public abstract class TenantBaseEntity implements TenantAware {

    public static final String TENANT_FILTER = "tenantFilter";
    public static final String TENANT_PARAM = "tenantId";
    public static final String TENANT_CONDITION = "institute_id = :tenantId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institute_id", nullable = false, updatable = false)
    private Long instituteId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getInstituteId() {
        return instituteId;
    }

    @Override
    public void setInstituteId(Long instituteId) {
        this.instituteId = instituteId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
