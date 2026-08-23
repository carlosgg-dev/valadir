package com.valadir.security.adapter;

import com.valadir.domain.model.AccountId;
import com.valadir.security.redis.RedisKeySpace;
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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class AccountTokensInvalidatorRedisAdapterIT {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    @Autowired
    private AccountTokensInvalidatorRedisAdapter accountTokensInvalidatorAdapter;

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
    void invalidateAll_multipleTokens_removesAllTokensAndUserSet() {

        var accountId = AccountId.generate();
        var token1 = UUID.randomUUID().toString();
        var token2 = UUID.randomUUID().toString();

        refreshTokenAdapter.save(token1, accountId);
        refreshTokenAdapter.save(token2, accountId);

        accountTokensInvalidatorAdapter.invalidateAll(accountId);

        assertThat(redisTemplate.hasKey(RedisKeySpace.forRefreshToken(token1))).isFalse();
        assertThat(redisTemplate.hasKey(RedisKeySpace.forRefreshToken(token2))).isFalse();
        assertThat(redisTemplate.hasKey(RedisKeySpace.forUserTokens(accountId.value().toString()))).isFalse();
    }

    @Test
    void invalidateAll_accountWithNoTokens_leavesNoState() {

        var accountId = AccountId.generate();

        accountTokensInvalidatorAdapter.invalidateAll(accountId);

        assertThat(redisTemplate.hasKey(RedisKeySpace.forUserTokens(accountId.value().toString()))).isFalse();
    }

    @Test
    void invalidateAll_account_cutsOffTheAccessTokensAlreadyIssued() {

        var accountId = AccountId.generate();
        var cutoffKey = RedisKeySpace.forTokenCutoff(accountId.value().toString());
        var beforeTheCall = Instant.now().getEpochSecond();

        accountTokensInvalidatorAdapter.invalidateAll(accountId);

        assertThat(redisTemplate.opsForValue().get(cutoffKey))
            .asLong()
            .isGreaterThanOrEqualTo(beforeTheCall);

        // Past the access token lifetime there is nothing left for the cutoff to reject.
        assertThat(redisTemplate.getExpire(cutoffKey))
            .isPositive()
            .isLessThanOrEqualTo(ACCESS_TOKEN_TTL.getSeconds());
    }

    @Test
    void invalidateAll_anotherAccountWithLiveSession_leavesItUntouched() {

        var accountId = AccountId.generate();
        var bystanderAccountId = AccountId.generate();
        var token = UUID.randomUUID().toString();
        var bystanderToken = UUID.randomUUID().toString();

        refreshTokenAdapter.save(token, accountId);
        refreshTokenAdapter.save(bystanderToken, bystanderAccountId);

        accountTokensInvalidatorAdapter.invalidateAll(accountId);

        assertThat(redisTemplate.opsForValue().get(RedisKeySpace.forRefreshToken(bystanderToken)))
            .isEqualTo(bystanderAccountId.value().toString());
        assertThat(redisTemplate.opsForSet().isMember(RedisKeySpace.forUserTokens(bystanderAccountId.value().toString()), bystanderToken))
            .isTrue();
        assertThat(redisTemplate.hasKey(RedisKeySpace.forTokenCutoff(bystanderAccountId.value().toString()))).isFalse();
    }
}
