package com.vidyapeet.prod;

import com.vidyapeet.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke check on the production configuration posture: it confirms that nothing in the
 * shipped config requires a paid hosting upgrade, so the platform stays within the
 * free tier (Render free web service + Vercel + Supabase free ~1GB).
 *
 * <p><b>Validates: Requirements 8.5</b> (operate on Render/Vercel/Supabase free tiers with
 * no paid upgrade) and reinforces <b>8.3</b> (Flyway-owned schema, {@code ddl-auto=validate})
 * and the free-tier storage budget angle of <b>8.4</b>.
 *
 * <p>This is a static parse of the shipped {@code application.yml} / {@code application-prod.yml}
 * and the storage policy constants — it needs no running context, so it is fast and never
 * touches a network or paid resource.
 */
class FreeTierPostureTest {

    private static Properties load(String resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resource));
        Properties properties = factory.getObject();
        assertThat(properties).as("%s should be present on the classpath", resource).isNotNull();
        return properties;
    }

    @Test
    void prodProfileRequiresNoPaidResources() {
        Properties prod = load("application-prod.yml");

        // Single managed PostgreSQL (available on Supabase/Render free tiers) — not a
        // proprietary/paid engine, and only ONE datasource is configured (no read replicas
        // or clustered/paid topologies).
        assertThat(prod.getProperty("spring.datasource.driver-class-name"))
                .as("prod uses a single standard PostgreSQL datasource")
                .isEqualTo("org.postgresql.Driver");
        assertThat(prod.stringPropertyNames())
                .as("no second datasource is configured (single free service)")
                .noneMatch(k -> k.contains("datasource.replica") || k.contains("datasource.secondary"));

        // Schema is Flyway-owned and validated, never mutated by Hibernate in prod.
        assertThat(prod.getProperty("spring.jpa.hibernate.ddl-auto"))
                .as("prod keeps ddl-auto=validate (Req 8.3)")
                .isEqualTo("validate");
        assertThat(prod.getProperty("spring.flyway.enabled"))
                .as("prod runs Flyway migrations (Req 8.3)")
                .isEqualTo("true");

        // Minimal actuator surface: health only. No paid observability/APM add-ons are wired.
        assertThat(prod.getProperty("management.endpoints.web.exposure.include"))
                .as("only the health endpoint is exposed in prod")
                .isEqualTo("health");

        // Nothing in the prod config pulls in a paid managed dependency.
        assertThat(prod.stringPropertyNames())
                .as("prod config must not require paid infra (cache clusters, brokers, etc.)")
                .noneMatch(k -> {
                    String key = k.toLowerCase();
                    return key.contains("redis") || key.contains("elasticache")
                            || key.contains("kafka") || key.contains("rabbitmq")
                            || key.contains("cluster");
                });
    }

    @Test
    void baseConfigTargetsASingleServiceAndPrivateSupabaseBucket() {
        Properties base = load("application.yml");

        // One web service bound to the platform-provided PORT (Render free web service).
        assertThat(base.getProperty("server.port"))
                .as("the server binds the platform-provided PORT (single Render service)")
                .contains("PORT");

        // Supabase storage is a single env-driven project/bucket; credentials are never
        // hard-coded (a private bucket accessed with the service key, no public CDN/paid tier).
        assertThat(base.getProperty("vidyapeet.storage.supabase.url"))
                .as("Supabase URL is supplied via environment, not hard-coded")
                .contains("SUPABASE_URL");
        assertThat(base.getProperty("vidyapeet.storage.supabase.service-key"))
                .as("Supabase service key is supplied via environment, not hard-coded")
                .contains("SUPABASE_SERVICE_KEY");
        assertThat(base.getProperty("vidyapeet.storage.supabase.bucket"))
                .as("a single Supabase bucket is configured")
                .contains("vidyapeet-files");
    }

    @Test
    void perImageSizeCapKeepsStorageWithinTheFreeTierBudget() {
        // A per-image cap bounds how fast the ~1GB Supabase free budget is consumed (Req 8.4/8.5).
        assertThat(StorageService.IMAGE_MAX_BYTES)
                .as("question images are capped per file so storage stays within the free tier")
                .isGreaterThan(0L)
                .isLessThanOrEqualTo(5L * 1024 * 1024);

        assertThat(StorageService.IMAGE_CONTENT_TYPES)
                .as("only compact web image formats are accepted")
                .isNotEmpty()
                .allMatch(type -> type.startsWith("image/"));
    }
}
