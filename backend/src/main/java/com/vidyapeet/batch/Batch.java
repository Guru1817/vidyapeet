package com.vidyapeet.batch;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "batches")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class Batch extends TenantBaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;
}
