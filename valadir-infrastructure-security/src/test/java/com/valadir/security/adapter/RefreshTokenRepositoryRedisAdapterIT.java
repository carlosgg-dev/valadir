package com.valadir.security.adapter;

import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
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
import java.util.Optional;
import java.util.UUID;

import static com.valadir.test.redis.RedisTestUtils.everythingStoredIn;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class RefreshTokenRepositoryRedisAdapterIT {

    // What Redis answers for a key that exists and carries no expiry.
    private static final long NO_EXPIRY = -1;

    @Autowired
    private RefreshTokenRepositoryRedisAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {

        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void validate_existingToken_returnsAccountId() {

        var accountId = AccountId.generate();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);
        Optional<AccountId> result = adapter.validate(token);

        assertThat(result).contains(accountId);
    }

    @Test
    void validate_nonExistingToken_returnsEmpty() {

        Optional<AccountId> result = adapter.validate(UUID.randomUUID().toString());

        assertThat(result).isEmpty();
    }

    @Test
    void save_token_isSavedWithAccountIdAndAddedToUserSet() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);

        var fingerprint = TokenFingerprint.of(token);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(fingerprint))).isEqualTo(accountIdStr);
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), fingerprint.value())).isTrue();
    }

    @Test
    void save_token_leavesItNowhereInRedis() {

        var token = UUID.randomUUID().toString();

        adapter.save(token, AccountId.generate());

        assertThat(everythingStoredIn(redisTemplate)).isNotEmpty().noneMatch(stored -> stored.contains(token));
    }

    @Test
    void save_token_boundsTheUserTokenSetLifetime() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var token = UUID.randomUUID().toString();

        adapter.save(token, accountId);

        Long tokenTtl = redisTemplate.getExpire(RedisKeySpace.forRefreshToken(TokenFingerprint.of(token)));

        assertThat(redisTemplate.getExpire(RedisKeySpace.forUserTokens(accountIdStr)))
            .isBetween(tokenTtl, jwtProperties.refreshTokenTtl().getSeconds());
    }

    @Test
    void save_secondSessionOnAnExpiringSet_extendsItToTheNewestToken() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var firstToken = UUID.randomUUID().toString();
        var secondToken = UUID.randomUUID().toString();
        var firstFingerprint = TokenFingerprint.of(firstToken);
        var secondFingerprint = TokenFingerprint.of(secondToken);
        String userTokensKey = RedisKeySpace.forUserTokens(accountIdStr);

        adapter.save(firstToken, accountId);

        // The set as it looks once the session that created it is about to expire. Without the
        // refresh, it would take the second token's fingerprint down with it a minute later.
        redisTemplate.expire(userTokensKey, Duration.ofMinutes(1));

        adapter.save(secondToken, accountId);

        Long secondTokenTtl = redisTemplate.getExpire(RedisKeySpace.forRefreshToken(secondFingerprint));

        assertThat(redisTemplate.getExpire(userTokensKey))
            .isBetween(secondTokenTtl, jwtProperties.refreshTokenTtl().getSeconds());
        assertThat(redisTemplate.opsForSet().members(userTokensKey))
            .containsExactlyInAnyOrder(firstFingerprint.value(), secondFingerprint.value());
    }

    @Test
    void rotate_existingToken_replacesOldWithNew() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var oldToken = UUID.randomUUID().toString();
        var newToken = UUID.randomUUID().toString();

        adapter.save(oldToken, accountId);

        boolean rotated = adapter.rotate(oldToken, newToken, accountId);

        var oldFingerprint = TokenFingerprint.of(oldToken);
        var newFingerprint = TokenFingerprint.of(newToken);

        assertThat(rotated).isTrue();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(oldFingerprint))).isNull();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(newFingerprint))).isEqualTo(accountIdStr);
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), oldFingerprint.value())).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(accountIdStr), newFingerprint.value())).isTrue();
    }

    @Test
    void rotate_nonExistingToken_returnsFalseAndLeavesNoState() {

        var accountId = AccountId.generate();
        var nonExistingToken = UUID.randomUUID().toString();
        var newToken = UUID.randomUUID().toString();

        boolean rotated = adapter.rotate(nonExistingToken, newToken, accountId);

        assertThat(rotated).isFalse();
        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(TokenFingerprint.of(newToken)))).isNull();
    }

    @Test
    void rotate_token_leavesNeitherTokenAnywhereInRedis() {

        var accountId = AccountId.generate();
        var oldToken = UUID.randomUUID().toString();
        var newToken = UUID.randomUUID().toString();

        adapter.save(oldToken, accountId);
        adapter.rotate(oldToken, newToken, accountId);

        assertThat(everythingStoredIn(redisTemplate))
            .isNotEmpty()
            .noneMatch(stored -> stored.contains(oldToken) || stored.contains(newToken));
    }

    @Test
    void rotate_tokenOnAPersistentSet_boundsItsLifetime() {

        var accountId = AccountId.generate();
        var accountIdStr = accountId.value().toString();
        var oldToken = UUID.randomUUID().toString();
        var newToken = UUID.randomUUID().toString();
        var oldFingerprint = TokenFingerprint.of(oldToken);
        var newFingerprint = TokenFingerprint.of(newToken);
        String userTokensKey = RedisKeySpace.forUserTokens(accountIdStr);

        redisTemplate.opsForValue().set(RedisKeySpace.forRefreshToken(oldFingerprint), accountIdStr, jwtProperties.refreshTokenTtl());
        redisTemplate.opsForSet().add(userTokensKey, oldFingerprint.value());

        // The scenario is the set reaching the rotation without an expiry. Were the seeding to stop
        // producing one, the rotation would create the set itself and this would silently degrade
        // into the save case.
        assertThat(redisTemplate.getExpire(userTokensKey)).isEqualTo(NO_EXPIRY);

        adapter.rotate(oldToken, newToken, accountId);

        Long newTokenTtl = redisTemplate.getExpire(RedisKeySpace.forRefreshToken(newFingerprint));

        assertThat(redisTemplate.getExpire(userTokensKey))
            .isBetween(newTokenTtl, jwtProperties.refreshTokenTtl().getSeconds());
    }
}
