package com.vidyapeet.config;

import com.vidyapeet.tenant.TenantAwareJpaRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers {@link TenantAwareJpaRepository} as the base class for every Spring
 * Data JPA repository, so tenant-safe {@code findById}/{@code existsById} is
 * applied everywhere without per-repository boilerplate.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.vidyapeet",
        repositoryBaseClass = TenantAwareJpaRepository.class
)
public class JpaConfig {
}
