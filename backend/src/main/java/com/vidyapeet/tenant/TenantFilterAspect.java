package com.vidyapeet.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Enables the Hibernate {@code tenantFilter} on the active session before any
 * repository method executes, scoping every query to the current tenant. This
 * enforces isolation at the data-access layer: even a mistaken or malicious
 * query in a service or controller cannot reach another institute's rows.
 *
 * <p>For SUPER_ADMIN cross-tenant operations the filter is intentionally not
 * enabled (see {@link TenantContext#isBypass()}).
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        if (TenantContext.isBypass()) {
            return;
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.getEnabledFilter(TenantBaseEntity.TENANT_FILTER);
        if (filter == null) {
            session.enableFilter(TenantBaseEntity.TENANT_FILTER)
                    .setParameter(TenantBaseEntity.TENANT_PARAM, tenantId);
        } else {
            filter.setParameter(TenantBaseEntity.TENANT_PARAM, tenantId);
        }
    }
}
