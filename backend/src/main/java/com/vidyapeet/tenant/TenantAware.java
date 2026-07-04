package com.vidyapeet.tenant;

/**
 * Marker for entities that belong to a single tenant (institute) and must be
 * scoped by {@code institute_id} at the data-access layer.
 */
public interface TenantAware {

    Long getInstituteId();

    void setInstituteId(Long instituteId);
}
