package com.valadir.security.adapter;

import com.valadir.domain.model.Email;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfig.class)
class LoginAttemptRepositoryRedisAdapterIT {

    private static final Email EMAIL = Email.from("bruce.wayne@email.com");

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(
        Duration.ofHours(1),
        2,
        List.of(
            new LoginLockoutThreshold(3, Duration.ofSeconds(30)),
            new LoginLockoutThreshold(5, Duration.ofSeconds(120)),
            new LoginLockoutThreshold(7, Duration.ofSeconds(600))
        )
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
    void findActiveLockout_withNoLockout_returnsEmpty() {

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
    }

    @Test
    void findActiveLockout_withActiveLockout_returnsTtl() {

        String lockoutKey = RedisKeySpace.forLoginLockout(EMAIL.value());
        redisTemplate.opsForValue().set(lockoutKey, "3", Duration.ofSeconds(30));

        assertThat(adapter.findActiveLockout(EMAIL))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(0).isLessThanOrEqualTo(30));
    }

    @Test
    void recordFailedAttempt_below3Failures_noLockout() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(EMAIL.value());

        adapter.recordFailedAttempt(EMAIL);
        adapter.recordFailedAttempt(EMAIL);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isEqualTo("2");
    }

    @Test
    void recordFailedAttempt_at3Failures_appliesShortLockout() {

        adapter.recordFailedAttempt(EMAIL);
        adapter.recordFailedAttempt(EMAIL);
        adapter.recordFailedAttempt(EMAIL);

        assertThat(adapter.findActiveLockout(EMAIL))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(0).isLessThanOrEqualTo(30));
    }

    @Test
    void recordFailedAttempt_at5Failures_applies2MinuteLockout() {

        IntStream.range(0, 5).forEach(i -> adapter.recordFailedAttempt(EMAIL));

        assertThat(adapter.findActiveLockout(EMAIL))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(60).isLessThanOrEqualTo(120));
    }

    @Test
    void recordFailedAttempt_at7Failures_appliesMaxLockout() {

        IntStream.range(0, 7).forEach(i -> adapter.recordFailedAttempt(EMAIL));

        assertThat(adapter.findActiveLockout(EMAIL))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(120).isLessThanOrEqualTo(600));
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

        assertThat(adapter.findActiveLockout(EMAIL)).isPresent();

        adapter.clearAttempts(EMAIL);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isNull();
    }
}
