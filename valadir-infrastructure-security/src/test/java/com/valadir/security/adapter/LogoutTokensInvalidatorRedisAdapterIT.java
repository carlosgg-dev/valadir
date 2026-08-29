package com.valadir.security.adapter;

import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.security.redis.TokenFingerprint;
import com.valadir.test.containers.RedisContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class LogoutTokensInvalidatorRedisAdapterIT {

    @Autowired
    private LogoutTokensInvalidatorRedisAdapter tokenInvalidatorAdapter;

    @Autowired
    private RefreshTokenRepositoryRedisAdapter refreshTokenAdapter;

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
        var jti = UUID.randomUUID().toString();
        var refreshToken = UUID.randomUUID().toString();
        Duration remainingTtl = Duration.ofMinutes(10);

        refreshTokenAdapter.save(refreshToken, accountId);

        tokenInvalidatorAdapter.invalidate(jti, remainingTtl, refreshToken, accountId);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);
        assertThat(redisTemplate.getExpire(RedisKeySpace.forBlacklist(jti))).isPositive();
        var fingerprint = TokenFingerprint.of(refreshToken);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(fingerprint))).isNull();
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountId.value().toString()), fingerprint.value())).isFalse();
    }

    @Test
    void invalidate_refreshTokenAlreadyGone_stillBlacklistsAccessToken() {

        var accountId = AccountId.generate();
        var jti = UUID.randomUUID().toString();
        var nonExistingRefreshToken = UUID.randomUUID().toString();
        Duration remainingTtl = Duration.ofMinutes(10);

        tokenInvalidatorAdapter.invalidate(jti, remainingTtl, nonExistingRefreshToken, accountId);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);
    }

    @Test
    void invalidate_refreshTokenOfAnotherAccount_keepsItAndStillBlacklists() {

        var loggingOutAccountId = AccountId.generate();
        var loggingOutJti = UUID.randomUUID().toString();
        var bystanderAccountId = AccountId.generate();
        var bystanderRefreshToken = UUID.randomUUID().toString();
        var remainingTtl = Duration.ofMinutes(10);

        // Only the bystander needs a live session: it is the state under protection, and without it
        // there would be nothing to survive the call, so the test could never fail.
        refreshTokenAdapter.save(bystanderRefreshToken, bystanderAccountId);

        tokenInvalidatorAdapter.invalidate(loggingOutJti, remainingTtl, bystanderRefreshToken, loggingOutAccountId);

        // Own logout still succeeds: a foreign token in the body must not keep the caller signed in.
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(loggingOutJti)))
            .isEqualTo(RedisKeySpace.BLACKLIST_REVOKED_VALUE);

        // The other account's session survives untouched — logging out must not reach across accounts.
        var bystanderFingerprint = TokenFingerprint.of(bystanderRefreshToken);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(bystanderFingerprint)))
            .isEqualTo(bystanderAccountId.value().toString());

        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(bystanderAccountId.value().toString()), bystanderFingerprint.value()))
            .isTrue();
    }

    @Test
    void invalidate_expiredAccessToken_skipsBlacklistButDeletesRefreshToken() {

        var accountId = AccountId.generate();
        var jti = UUID.randomUUID().toString();
        var refreshToken = UUID.randomUUID().toString();

        refreshTokenAdapter.save(refreshToken, accountId);

        tokenInvalidatorAdapter.invalidate(jti, Duration.ZERO, refreshToken, accountId);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forBlacklist(jti))).isNull();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(TokenFingerprint.of(refreshToken)))).isNull();
    }
}
