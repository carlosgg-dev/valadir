package com.valadir.security.adapter;

import com.valadir.domain.model.AccountId;
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
class PasswordResetVerificationTokenStoreRedisAdapterTest extends RedisTestContainer {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private PasswordResetVerificationTokenStoreRedisAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new PasswordResetVerificationTokenStoreRedisAdapter(redisTemplate);
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void save_storesAccountIdInRedis() {

        var accountId = AccountId.generate();
        var token = UUID.randomUUID().toString();
        String redisKey = RedisKeySpace.forPasswordResetVerificationToken(token);

        adapter.save(token, accountId, TOKEN_TTL);

        assertThat(redisTemplate.opsForValue().get(redisKey)).isEqualTo(accountId.value().toString());
        assertThat(redisTemplate.getExpire(redisKey)).isGreaterThan(0);
    }

    @Test
    void resolveAccountId_existingToken_returnsAccountId() {

        var accountId = AccountId.generate();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId, TOKEN_TTL);

        assertThat(adapter.resolveAccountId(token)).contains(accountId);
    }

    @Test
    void resolveAccountId_nonExistingToken_returnsEmpty() {

        var token = UUID.randomUUID().toString();

        assertThat(adapter.resolveAccountId(token)).isEmpty();
    }

    @Test
    void delete_existingToken_removesItFromRedis() {

        var accountId = AccountId.generate();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId, TOKEN_TTL);

        assertThat(adapter.resolveAccountId(token)).contains(accountId);

        adapter.delete(token);

        assertThat(adapter.resolveAccountId(token)).isEmpty();
    }
}
