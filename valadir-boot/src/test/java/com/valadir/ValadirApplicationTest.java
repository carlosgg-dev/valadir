package com.valadir;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@ActiveProfiles("test")
class ValadirApplicationTest {

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final RedisContainer REDIS;

    static {
        POSTGRES = createPostgreSQLContainer();
        POSTGRES.start();

        REDIS = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));
        REDIS.start();
    }

    @SuppressWarnings("resource")
    private static PostgreSQLContainer<?> createPostgreSQLContainer() {

        return new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("valadir_db")
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
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    }

    @Test
    void contextLoads() {

    }
}
