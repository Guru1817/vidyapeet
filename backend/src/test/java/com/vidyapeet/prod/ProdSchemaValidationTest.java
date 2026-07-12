package com.vidyapeet.prod;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application under the {@code prod} profile against a Flyway-migrated
 * schema and lets Hibernate {@code ddl-auto=validate} run, asserting there is no drift
 * between the JPA entities and the migrated schema.
 *
 * <p><b>Validates: Requirements 8.3</b> (prod keeps {@code ddl-auto=validate} and the
 * schema is owned by Flyway migrations) and supports <b>8.5</b> (the prod profile boots
 * without requiring any paid resource — see {@link FreeTierPostureTest} for the config
 * posture assertions).
 *
 * <h2>How this achieves a faithful prod-profile boot</h2>
 * The {@code prod} profile is activated verbatim, so its real settings apply:
 * {@code spring.jpa.hibernate.ddl-auto=validate}, {@code spring.flyway.enabled=true},
 * {@code open-in-view=false}, and health-only actuator exposure. Flyway runs the schema
 * <em>before</em> Hibernate initialises (Spring Boot orders the {@code EntityManagerFactory}
 * after Flyway), then Hibernate validates every mapped entity against it. If any entity
 * column/table is missing or has an incompatible type, the {@code EntityManagerFactory}
 * fails and this test fails to load its context. A green run therefore certifies
 * entity&lt;-&gt;schema agreement under the production configuration.
 *
 * <h2>Why the datasource is H2 (and the migration is reconstructed)</h2>
 * Production runs PostgreSQL, but this environment has no Docker/Testcontainers/PostgreSQL
 * (the same constraint documented on the V7 backfill test). The real migrations
 * {@code V1..V6} also use PostgreSQL-only spellings H2 cannot parse ({@code TIMESTAMPTZ},
 * comma-separated multi-column {@code ADD COLUMN}, {@code ALTER COLUMN ... TYPE ... USING}),
 * so they cannot be replayed here. To still exercise the prod {@code validate} path, the
 * datasource is overridden to H2 in PostgreSQL-compatibility mode and Flyway is pointed at
 * {@code classpath:db/prodvalidation}, which holds a single H2-parseable migration that
 * reproduces the <em>cumulative end state</em> of the real {@code V1..V8} migrations
 * (derived from those migration files, independent of the entities). The rest of the prod
 * configuration is untouched.
 *
 * <p>Dummy Supabase credentials are supplied so the {@code @Profile("prod")}
 * {@code SupabaseStorageService} bean can be constructed (it only builds a REST client at
 * construction time and makes no network call during boot).
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The schema is a reconstruction of the migrations' end state rather than the raw
 *       {@code V1..V8} files replayed verbatim, and it runs on H2 rather than PostgreSQL.
 *       This certifies entity&lt;-&gt;schema drift (Req 8.3), not PostgreSQL type-name
 *       compatibility of the raw migration SQL.</li>
 *   <li>{@code src/test/resources/db/prodvalidation/V1__prod_equivalent_schema.sql} must be
 *       kept in lockstep with the real migrations when the schema changes; the header of
 *       that file documents the derivation.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        // Override the prod PostgreSQL datasource with H2 in PostgreSQL-compatibility mode.
        "spring.datasource.url=jdbc:h2:mem:prodvalidate;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Run the H2-parseable reconstruction of the V1..V8 end state instead of the real
        // PostgreSQL-only migrations, then let prod's ddl-auto=validate check the entities.
        "spring.flyway.locations=classpath:db/prodvalidation",
        // Dummy Supabase settings so the @Profile("prod") storage bean can be constructed.
        "vidyapeet.storage.supabase.url=http://localhost/supabase",
        "vidyapeet.storage.supabase.service-key=test-service-key",
        // Never seed in this validation boot.
        "vidyapeet.seed.enabled=false"
})
@ActiveProfiles("prod")
class ProdSchemaValidationTest {

    @Autowired
    private Environment environment;

    /**
     * The context loading at all means Flyway migrated the schema and Hibernate's
     * {@code validate} passed against it (no entity/schema drift). We additionally assert
     * the prod invariants that make this a meaningful prod-profile boot.
     */
    @Test
    void prodProfileBootsWithValidateAgainstMigratedSchema() {
        // ddl-auto stays validate in prod: Hibernate never mutates the Flyway-owned schema.
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .as("prod must keep ddl-auto=validate (Req 8.3)")
                .isEqualTo("validate");

        // Flyway owns the schema in prod.
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class, true))
                .as("prod must run Flyway migrations (Req 8.3)")
                .isTrue();

        // The prod profile is the one actually active for this boot.
        assertThat(environment.getActiveProfiles())
                .as("this validation must run under the prod profile")
                .contains("prod");
    }
}
