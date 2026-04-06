package com.valadir.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public abstract class PostgresTestContainer {

    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = createContainer();
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
}
