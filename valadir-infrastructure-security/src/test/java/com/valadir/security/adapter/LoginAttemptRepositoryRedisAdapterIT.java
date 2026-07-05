package com.valadir.security.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.domain.policy.LoginLockoutThreshold;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class LoginAttemptRepositoryRedisAdapterIT {

    private static final Email EMAIL = Email.from("bruce.wayne@email.com");

    private static final int CHALLENGE_THRESHOLD = 2;

    private static final LoginLockoutThreshold FIRST_LOCKOUT_TIER = new LoginLockoutThreshold(3, Duration.ofSeconds(30));

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(
        Duration.ofHours(1),
        CHALLENGE_THRESHOLD,
        List.of(FIRST_LOCKOUT_TIER)
    );

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private LoginAttemptRepositoryRedisAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new LoginAttemptRepositoryRedisAdapter(redisTemplate, POLICY);
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void evaluate_withActiveLockout_returnsLockedOutWithRemainingTtl() {

        String lockoutKey = RedisKeySpace.forLoginLockout(EMAIL.value());
        redisTemplate.opsForValue().set(lockoutKey, RedisKeySpace.LOGIN_LOCKOUT_VALUE, Duration.ofSeconds(30));

        LoginAttemptDecision decision = adapter.evaluate(EMAIL);

        assertThat(decision).isInstanceOf(LoginAttemptDecision.LockedOut.class);
        Duration remaining = ((LoginAttemptDecision.LockedOut) decision).remaining();
        assertThat(remaining.toSeconds()).isGreaterThan(0).isLessThanOrEqualTo(30);
    }

    @Test
    void recordFailedAttempt_firstFailure_startsAttemptsWindow() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(EMAIL.value());

        adapter.recordFailedAttempt(EMAIL);

        Long ttl = redisTemplate.getExpire(attemptsKey, TimeUnit.SECONDS);
        long windowSeconds = POLICY.attemptsWindow().toSeconds();
        assertThat(ttl).isGreaterThan(windowSeconds - 5).isLessThanOrEqualTo(windowSeconds);
    }

    @Test
    void evaluate_afterRecordedFailures_returnsPolicyDecisionForStoredCount() {

        int recordedFailures = CHALLENGE_THRESHOLD;
        IntStream.range(0, recordedFailures).forEach(i -> adapter.recordFailedAttempt(EMAIL));

        assertThat(adapter.evaluate(EMAIL)).isEqualTo(POLICY.decideByCount(recordedFailures));
    }

    @Test
    void evaluate_noAttempts_returnsAllowed() {

        assertThat(adapter.evaluate(EMAIL)).isInstanceOf(LoginAttemptDecision.Allowed.class);
    }

    @Test
    void recordFailedAttempt_belowFirstLockoutTier_returnsEmptyAndAccumulatesCount() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(EMAIL.value());
        int belowTierFailures = FIRST_LOCKOUT_TIER.minFailures() - 1;

        IntStream.range(0, belowTierFailures - 1).forEach(i -> adapter.recordFailedAttempt(EMAIL));
        Optional<Duration> lockout = adapter.recordFailedAttempt(EMAIL);

        assertThat(lockout).isEmpty();
        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isEqualTo(String.valueOf(belowTierFailures));
    }

    @Test
    void recordFailedAttempt_reachesFirstLockoutTier_returnsFirstTierLockout() {

        IntStream.range(0, FIRST_LOCKOUT_TIER.minFailures() - 1).forEach(i -> adapter.recordFailedAttempt(EMAIL));
        Optional<Duration> lockout = adapter.recordFailedAttempt(EMAIL);

        assertThat(lockout).hasValue(FIRST_LOCKOUT_TIER.lockout());
    }

    @Test
    void recordFailedAttempt_subsequentIncrement_doesNotRefreshWindow() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(EMAIL.value());
        Duration elapsedWindow = Duration.ofSeconds(10);

        // Simulate a window already in progress: counter present with a short remaining TTL.
        redisTemplate.opsForValue().set(attemptsKey, "1", elapsedWindow);

        adapter.recordFailedAttempt(EMAIL);

        Long ttl = redisTemplate.getExpire(attemptsKey, TimeUnit.SECONDS);
        assertThat(ttl)
            .isPositive()
            .isLessThanOrEqualTo(elapsedWindow.toSeconds());

        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isEqualTo("2");
    }

    @Test
    void clearAttempts_removesCounterAndLockout() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(EMAIL.value());

        adapter.recordFailedAttempt(EMAIL);
        adapter.recordFailedAttempt(EMAIL);
        adapter.recordFailedAttempt(EMAIL);

        assertThat(adapter.evaluate(EMAIL)).isInstanceOf(LoginAttemptDecision.LockedOut.class);

        adapter.clearAttempts(EMAIL);

        assertThat(adapter.evaluate(EMAIL)).isInstanceOf(LoginAttemptDecision.Allowed.class);
        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isNull();
    }
}
