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
class AccessTokenRevocationRedisAdapterIT {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    @Autowired
    private AccessTokenRevocationRedisAdapter adapter;

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

        String accountId = accountId();
        String jti = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RedisKeySpace.forBlacklist(jti), RedisKeySpace.BLACKLIST_REVOKED_VALUE, ACCESS_TOKEN_TTL);
        Instant issuedAt = Instant.now();

        assertThat(adapter.isRevoked(jti, accountId, issuedAt)).isTrue();
    }

    @Test
    void isRevoked_nonRevokedJti_returnsFalse() {

        String accountId = accountId();
        String jti = UUID.randomUUID().toString();
        Instant issuedAt = Instant.now();

        assertThat(adapter.isRevoked(jti, accountId, issuedAt)).isFalse();
    }

    @Test
    void isRevoked_tokenIssuedBeforeTheAccountCutoff_returnsTrue() {

        String accountId = accountId();
        Instant cutoff = Instant.now();
        storeCutoff(accountId, cutoff);
        String jti = UUID.randomUUID().toString();
        Instant issuedAt = cutoff.minusSeconds(1);

        assertThat(adapter.isRevoked(jti, accountId, issuedAt)).isTrue();
    }

    @Test
    void isRevoked_tokenIssuedWithinTheCutoffSecond_returnsTrue() {

        String accountId = accountId();
        Instant cutoff = Instant.now();
        storeCutoff(accountId, cutoff);
        String jti = UUID.randomUUID().toString();

        assertThat(adapter.isRevoked(jti, accountId, cutoff)).isTrue();
    }

    @Test
    void isRevoked_tokenIssuedAfterTheAccountCutoff_returnsFalse() {

        String accountId = accountId();
        Instant cutoff = Instant.now();
        storeCutoff(accountId, cutoff);
        String jti = UUID.randomUUID().toString();
        Instant issuedAt = cutoff.plusSeconds(1);

        assertThat(adapter.isRevoked(jti, accountId, issuedAt)).isFalse();
    }

    @Test
    void isRevoked_cutoffOfAnotherAccount_returnsFalse() {

        String accountId = accountId();
        String bystanderAccountId = accountId();
        Instant cutoff = Instant.now();
        storeCutoff(accountId, cutoff);
        String jti = UUID.randomUUID().toString();
        Instant issuedAt = cutoff.minusSeconds(1);

        assertThat(adapter.isRevoked(jti, bystanderAccountId, issuedAt)).isFalse();
    }

    private static String accountId() {

        return AccountId.generate().value().toString();
    }

    private void storeCutoff(String accountId, Instant cutoff) {

        redisTemplate.opsForValue().set(
            RedisKeySpace.forTokenCutoff(accountId),
            String.valueOf(cutoff.getEpochSecond()),
            ACCESS_TOKEN_TTL
        );
    }
}
