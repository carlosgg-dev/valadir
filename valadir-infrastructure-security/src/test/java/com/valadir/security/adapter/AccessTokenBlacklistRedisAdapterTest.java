package com.valadir.security.adapter;

import com.valadir.security.RedisTestContainer;
import com.valadir.security.redis.RedisKeySpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
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

        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void isRevoked_revokedJti_returnsTrue() {

        String jti = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RedisKeySpace.forBlacklist(jti), RedisKeySpace.BLACKLIST_REVOKED_VALUE, Duration.ofSeconds(900));

        assertThat(adapter.isRevoked(jti)).isTrue();
    }

    @Test
    void isRevoked_nonRevokedJti_returnsFalse() {

        assertThat(adapter.isRevoked(UUID.randomUUID().toString())).isFalse();
    }
}
