package com.vidyapeet.tenant;

import com.vidyapeet.batch.Batch;
import com.vidyapeet.batch.repository.BatchRepository;
import com.vidyapeet.institute.Institute;
import com.vidyapeet.institute.repository.InstituteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Hibernate tenant filter, enabled by {@link TenantFilterAspect},
 * scopes every query to the current institute so one tenant can never read
 * another tenant's rows.
 *
 * <p>Queries run inside a transaction (via {@link TransactionTemplate}) to mirror
 * how the application accesses data: controller -> {@code @Transactional} service
 * -> repository. The filter is enabled on the active transactional session.
 */
@SpringBootTest(properties = "vidyapeet.seed.enabled=false")
@ActiveProfiles("dev")
class TenantIsolationTest {

    @Autowired
    private InstituteRepository instituteRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    private Long instituteA;
    private Long instituteB;
    private Long batchInBId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        TenantContext.clear();

        // The H2 instance is shared across tests (no rollback); start clean.
        TenantContext.setBypass(true);
        batchRepository.deleteAll();
        instituteRepository.deleteAll();
        TenantContext.clear();

        instituteA = createInstitute("Alpha Academy", "alpha");
        instituteB = createInstitute("Beta Institute", "beta");

        // Two batches for tenant A.
        runAsTenant(instituteA, () -> {
            saveBatch("A - Maths");
            saveBatch("A - Science");
        });

        // One batch for tenant B.
        runAsTenant(instituteB, () -> batchInBId = saveBatch("B - History"));

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void tenantSeesOnlyItsOwnRows() {
        List<Batch> aBatches = readAsTenant(instituteA, () -> batchRepository.findAll());
        assertThat(aBatches).hasSize(2);
        assertThat(aBatches).allMatch(b -> b.getInstituteId().equals(instituteA));

        List<Batch> bBatches = readAsTenant(instituteB, () -> batchRepository.findAll());
        assertThat(bBatches).hasSize(1);
        assertThat(bBatches).allMatch(b -> b.getInstituteId().equals(instituteB));
    }

    @Test
    void tenantCannotLoadAnotherTenantsRowById() {
        // batchInBId belongs to tenant B; tenant A must not be able to read it.
        Optional<Batch> result = readAsTenant(instituteA, () -> batchRepository.findById(batchInBId));
        assertThat(result).isEmpty();
    }

    @Test
    void bypassSeesAllTenants() {
        TenantContext.setBypass(true);
        try {
            List<Batch> all = tx.execute(status -> batchRepository.findAll());
            assertThat(all).hasSize(3);
        } finally {
            TenantContext.clear();
        }
    }

    private Long createInstitute(String name, String slug) {
        Institute institute = new Institute();
        institute.setName(name);
        institute.setSlug(slug);
        institute.setPrimaryColor("#000000");
        return instituteRepository.save(institute).getId();
    }

    private Long saveBatch(String name) {
        Batch batch = new Batch();
        batch.setName(name);
        return batchRepository.save(batch).getId();
    }

    private void runAsTenant(Long tenantId, Runnable action) {
        TenantContext.setTenantId(tenantId);
        try {
            action.run();
        } finally {
            TenantContext.clear();
        }
    }

    private <T> T readAsTenant(Long tenantId, Supplier<T> query) {
        TenantContext.setTenantId(tenantId);
        try {
            return tx.execute(status -> query.get());
        } finally {
            TenantContext.clear();
        }
    }
}
