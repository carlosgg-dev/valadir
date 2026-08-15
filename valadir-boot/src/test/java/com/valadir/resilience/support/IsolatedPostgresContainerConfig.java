package com.valadir.resilience.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * A Postgres container owned by the resilience suite, separate from the shared one in
 * {@code valadir-test-support}, for the same reason as {@link IsolatedRedisContainerConfig}:
 * it exists to be paused, and pausing the shared one would poison every other IT in the fork.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IsolatedPostgresContainerConfig {

    private static final PostgreSQLContainer<?> POSTGRES = createContainer();

    static {
        POSTGRES.start();
    }

    public static PostgreSQLContainer<?> container() {

        return POSTGRES;
    }

    @SuppressWarnings("resource")
    private static PostgreSQLContainer<?> createContainer() {

        return new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("valadir")
            .withUsername("valadir_user")
            .withPassword("valadir_password")
            .withCopyFileToContainer(
                MountableFile.forHostPath("../docker/postgres/init.sql"),
                "/docker-entrypoint-initdb.d/init.sql"
            );
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> isolatedPostgresContainer() {

        return POSTGRES;
    }
}
