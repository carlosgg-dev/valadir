package com.valadir.test.containers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    public static final PostgreSQLContainer<?> POSTGRES = createContainer();

    static {
        POSTGRES.start();
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
    PostgreSQLContainer<?> postgresContainer() {

        return POSTGRES;
    }
}
