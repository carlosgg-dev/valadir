package com.valadir.security.adapter;

import com.valadir.application.port.out.PasswordResetVerification;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
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

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static com.valadir.test.redis.RedisTestUtils.everythingStoredIn;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class PasswordResetVerificationTokenRepositoryRedisAdapterIT {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private static final Email EMAIL = Email.from("bruce.wayne@email.com");

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private PasswordResetVerificationTokenRepositoryRedisAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new PasswordResetVerificationTokenRepositoryRedisAdapter(redisTemplate, buildClosedCircuitGuard());
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void save_verification_expiresWithinTheTtl() {

        var token = UUID.randomUUID().toString();
        var verification = new PasswordResetVerification(AccountId.generate(), EMAIL);

        adapter.save(token, verification, TOKEN_TTL);

        assertThat(redisTemplate.getExpire(redisKeyOf(token))).isBetween(1L, TOKEN_TTL.toSeconds());
    }

    @Test
    void save_token_leavesItNowhereInRedis() {

        var token = UUID.randomUUID().toString();
        var verification = new PasswordResetVerification(AccountId.generate(), EMAIL);

        adapter.save(token, verification, TOKEN_TTL);

        assertThat(everythingStoredIn(redisTemplate)).isNotEmpty().noneMatch(stored -> stored.contains(token));
    }

    @Test
    void verificationFor_savedVerification_returnsTheAccountAndTheEmailTogether() {

        var token = UUID.randomUUID().toString();
        var verification = new PasswordResetVerification(AccountId.generate(), EMAIL);

        adapter.save(token, verification, TOKEN_TTL);

        assertThat(adapter.verificationFor(token)).contains(verification);
    }

    @Test
    void verificationFor_nonExistingToken_returnsEmpty() {

        assertThat(adapter.verificationFor(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void verificationFor_verificationMissingTheAccountId_returnsEmpty() {

        var token = UUID.randomUUID().toString();

        redisTemplate.<String, String>opsForHash().put(redisKeyOf(token), "email", EMAIL.value());

        assertThat(adapter.verificationFor(token)).isEmpty();
    }

    @Test
    void verificationFor_verificationMissingTheEmail_returnsEmpty() {

        var token = UUID.randomUUID().toString();

        redisTemplate.<String, String>opsForHash().put(redisKeyOf(token), "account_id", AccountId.generate().value().toString());

        assertThat(adapter.verificationFor(token)).isEmpty();
    }

    @Test
    void delete_existingToken_removesIt() {

        var token = UUID.randomUUID().toString();
        var verification = new PasswordResetVerification(AccountId.generate(), EMAIL);

        adapter.save(token, verification, TOKEN_TTL);
        adapter.delete(token);

        assertThat(redisTemplate.hasKey(redisKeyOf(token))).isFalse();
    }

    private static String redisKeyOf(String token) {

        return RedisKeySpace.forPasswordResetVerificationToken(TokenFingerprint.of(token));
    }
}
