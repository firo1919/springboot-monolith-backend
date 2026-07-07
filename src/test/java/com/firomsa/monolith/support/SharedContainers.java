package com.firomsa.monolith.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Holds the single Postgres container shared by every integration and e2e test
 * in the JVM.
 *
 * <p>
 * Started once on class load and reaped by Testcontainers' Ryuk sidecar at JVM
 * exit. Sharing one
 * container (instead of one per test base class) keeps the footprint small
 * enough for memory-limited
 * CI runners and avoids the per-class container pile-up that previously
 * OOM-killed the runner.
 * Container reuse ({@code withReuse}) is deliberately not used: it is only
 * honored on developer
 * machines and must not be relied on in CI.
 */
public final class SharedContainers {

    @SuppressWarnings("resource")
    public static final PostgreSQLContainer POSTGRES = (PostgreSQLContainer) new PostgreSQLContainer("postgres:18-alpine")
            .withStartupTimeout(java.time.Duration.ofMinutes(3));

    static {
        POSTGRES.start();
    }

    private SharedContainers() {
    }
}
