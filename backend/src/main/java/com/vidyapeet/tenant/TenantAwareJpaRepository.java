package com.vidyapeet.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.Optional;

/**
 * Base repository for all Spring Data JPA repositories. It closes a subtle gap in
 * Hibernate's {@code @Filter}: filters apply to queries but NOT to direct
 * primary-key loads ({@code EntityManager.find}). For tenant-scoped entities,
 * this routes {@code findById}/{@code existsById} through a filtered criteria
 * query so a caller can never load another tenant's row by guessing its id.
 *
 * <p>Non-tenant entities (Institute, User) fall through to the default behaviour.
 */
public class TenantAwareJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public TenantAwareJpaRepository(JpaEntityInformation<T, ?> entityInformation,
                                    EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (!isTenantScoped() || TenantContext.isBypass() || !TenantContext.hasTenant()) {
            return super.findById(id);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(getDomainClass());
        Root<T> root = query.from(getDomainClass());
        String idName = entityInformation.getIdAttribute().getName();
        query.select(root).where(cb.equal(root.get(idName), id));
        return entityManager.createQuery(query).getResultList().stream().findFirst();
    }

    @Override
    public boolean existsById(ID id) {
        if (!isTenantScoped() || TenantContext.isBypass() || !TenantContext.hasTenant()) {
            return super.existsById(id);
        }
        return findById(id).isPresent();
    }

    private boolean isTenantScoped() {
        return TenantAware.class.isAssignableFrom(getDomainClass());
    }
}
