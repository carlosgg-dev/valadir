package com.valadir;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

@SpringBootTest
@ActiveProfiles("test")
class ValadirApplicationTest {

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final RedisContainer REDIS;
    private static final ECKey EC_KEY;

    static {
        try {
            POSTGRES = createPostgreSQLContainer();
            POSTGRES.start();

            REDIS = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));
            REDIS.start();

            EC_KEY = buildEcKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private static ECKey buildEcKey() throws NoSuchAlgorithmException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair keyPair = kpg.generateKeyPair();

        return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
            .privateKey((ECPrivateKey) keyPair.getPrivate())
            .build();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("jwt.private-key", EC_KEY::toJSONString);
    }

    @Test
    void contextLoads() {

    }
}
