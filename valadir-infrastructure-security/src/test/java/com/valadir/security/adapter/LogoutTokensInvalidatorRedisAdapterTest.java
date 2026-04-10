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

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LogoutTokensInvalidatorRedisAdapterTest extends RedisTestContainer {

    @Autowired
    private LogoutTokensInvalidatorRedisAdapter tokenInvalidatorAdapter;

    @Autowired
    private RefreshTokenRedisAdapter refreshTokenAdapter;

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
    void invalidate_blacklistsAccessTokenAndDeletesRefreshToken() {

        final var accountId = AccountId.generate();
        final var accountIdStr = accountId.value().toString();
        final var jti = UUID.randomUUID().toString();
        final var refreshToken = UUID.randomUUID().toString();
        final long ttl = 600L;

        refreshTokenAdapter.save(refreshToken, accountId);

        tokenInvalidatorAdapter.invalidate(jti, ttl, refreshToken, accountIdStr);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);
        assertThat(redisTemplate.getExpire(RedisKeySpace.forBlacklist(jti))).isPositive();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(refreshToken))).isNull();
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), refreshToken)).isFalse();
    }

    @Test
    void invalidate_refreshTokenAlreadyGone_stillBlacklistsAccessToken() {

        final var accountId = AccountId.generate();
        final var accountIdStr = accountId.value().toString();
        final var jti = UUID.randomUUID().toString();
        final var nonExistingRefreshToken = UUID.randomUUID().toString();
        final long ttl = 600L;

        tokenInvalidatorAdapter.invalidate(jti, ttl, nonExistingRefreshToken, accountIdStr);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);
    }

    @Test
    void invalidate_expiredAccessToken_skipsBlacklistButDeletesRefreshToken() {

        final var accountId = AccountId.generate();
        final var accountIdStr = accountId.value().toString();
        final var jti = UUID.randomUUID().toString();
        final var refreshToken = UUID.randomUUID().toString();

        refreshTokenAdapter.save(refreshToken, accountId);

        tokenInvalidatorAdapter.invalidate(jti, 0L, refreshToken, accountIdStr);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isNull();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(refreshToken))).isNull();
    }
}
