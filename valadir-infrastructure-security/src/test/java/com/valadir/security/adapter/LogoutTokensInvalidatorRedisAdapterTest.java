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
class LogoutTokensInvalidatorRedisAdapterTest extends RedisTestContainer {

    @Autowired
    private LogoutTokensInvalidatorRedisAdapter tokenInvalidatorAdapter;

    @Autowired
    private RefreshTokenRedisAdapter refreshTokenAdapter;

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
    void invalidate_blacklistsAccessTokenAndDeletesRefreshToken() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var jti = UUID.randomUUID().toString();
        var refreshToken = UUID.randomUUID().toString();
        Duration remainingTtl = Duration.ofMinutes(10);

        refreshTokenAdapter.save(refreshToken, accountId);

        tokenInvalidatorAdapter.invalidate(jti, remainingTtl, refreshToken, accountIdStr);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);
        assertThat(redisTemplate.getExpire(RedisKeySpace.forBlacklist(jti))).isPositive();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(refreshToken))).isNull();
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), refreshToken)).isFalse();
    }

    @Test
    void invalidate_refreshTokenAlreadyGone_stillBlacklistsAccessToken() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var jti = UUID.randomUUID().toString();
        var nonExistingRefreshToken = UUID.randomUUID().toString();
        Duration remainingTtl = Duration.ofMinutes(10);

        tokenInvalidatorAdapter.invalidate(jti, remainingTtl, nonExistingRefreshToken, accountIdStr);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);
    }

    @Test
    void invalidate_expiredAccessToken_skipsBlacklistButDeletesRefreshToken() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var jti = UUID.randomUUID().toString();
        var refreshToken = UUID.randomUUID().toString();

        refreshTokenAdapter.save(refreshToken, accountId);

        tokenInvalidatorAdapter.invalidate(jti, Duration.ZERO, refreshToken, accountIdStr);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isNull();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(refreshToken))).isNull();
    }
}
