package com.vidyapeet.tenant;

import jakarta.persistence.PrePersist;

/**
 * Stamps the current tenant id onto a {@link TenantAware} entity at insert time,
 * so application code never has to set {@code institute_id} manually. If the
 * entity already carries an institute id (e.g. set explicitly during seeding),
 * that value is preserved.
 */
public class TenantEntityListener {

    @PrePersist
    public void setTenantOnPersist(Object entity) {
        if (entity instanceof TenantAware tenantAware && tenantAware.getInstituteId() == null) {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                tenantAware.setInstituteId(tenantId);
            }
        }
    }
}
