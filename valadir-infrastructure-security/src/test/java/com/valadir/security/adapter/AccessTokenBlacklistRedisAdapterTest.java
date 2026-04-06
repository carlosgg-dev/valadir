package com.valadir.security.adapter;

import com.valadir.security.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AccessTokenBlacklistRedisAdapterTest extends RedisTestContainer {

    @Autowired
    private AccessTokenBlacklistRedisAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {

        final RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (final var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void revoke_jti_isStoredWithTtl() {

        final String jti = UUID.randomUUID().toString();

        adapter.revoke(jti, 900L);

        assertThat(adapter.isRevoked(jti)).isTrue();
        assertThat(redisTemplate.getExpire("blacklist:" + jti)).isPositive();
    }

    @Test
    void isRevoked_revokedJti_returnsTrue() {

        final String jti = UUID.randomUUID().toString();

        adapter.revoke(jti, 900L);

        assertThat(adapter.isRevoked(jti)).isTrue();
    }

    @Test
    void isRevoked_nonRevokedJti_returnsFalse() {

        assertThat(adapter.isRevoked(UUID.randomUUID().toString())).isFalse();
    }
}
