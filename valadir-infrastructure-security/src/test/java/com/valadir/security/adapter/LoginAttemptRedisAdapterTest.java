package com.valadir.security.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.domain.policy.LoginLockoutThreshold;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoginAttemptRedisAdapterTest extends RedisTestContainer {

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(
        Duration.ofHours(1),
        List.of(
            new LoginLockoutThreshold(3, Duration.ofSeconds(30)),
            new LoginLockoutThreshold(5, Duration.ofSeconds(120)),
            new LoginLockoutThreshold(7, Duration.ofSeconds(600))
        )
    );

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private LoginAttemptRedisAdapter adapter;

    private final Email email = new Email("bruce.wayne@email.com");

    @BeforeEach
    void setUp() {

        adapter = new LoginAttemptRedisAdapter(redisTemplate, POLICY);
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (var connection = factory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void findActiveLockout_withNoLockout_returnsEmpty() {

        assertThat(adapter.findActiveLockout(email)).isEmpty();
    }

    @Test
    void findActiveLockout_withActiveLockout_returnsTtl() {

        String lockoutKey = RedisKeySpace.forLoginLockout(email.value());
        redisTemplate.opsForValue().set(lockoutKey, "3", Duration.ofSeconds(30));

        assertThat(adapter.findActiveLockout(email))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(0).isLessThanOrEqualTo(30));
    }

    @Test
    void recordFailedAttempt_below3Failures_noLockout() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(email.value());

        adapter.recordFailedAttempt(email);
        adapter.recordFailedAttempt(email);

        assertThat(adapter.findActiveLockout(email)).isEmpty();
        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isEqualTo("2");
    }

    @Test
    void recordFailedAttempt_at3Failures_appliesShortLockout() {

        adapter.recordFailedAttempt(email);
        adapter.recordFailedAttempt(email);
        adapter.recordFailedAttempt(email);

        assertThat(adapter.findActiveLockout(email))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(0).isLessThanOrEqualTo(30));
    }

    @Test
    void recordFailedAttempt_at5Failures_applies2MinuteLockout() {

        IntStream.range(0, 5).forEach(i -> adapter.recordFailedAttempt(email));

        assertThat(adapter.findActiveLockout(email))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(60).isLessThanOrEqualTo(120));
    }

    @Test
    void recordFailedAttempt_at7Failures_appliesMaxLockout() {

        IntStream.range(0, 7).forEach(i -> adapter.recordFailedAttempt(email));

        assertThat(adapter.findActiveLockout(email))
            .isPresent()
            .hasValueSatisfying(ttl -> assertThat(ttl.toSeconds()).isGreaterThan(120).isLessThanOrEqualTo(600));
    }

    @Test
    void clearAttempts_removesCounterAndLockout() {

        String attemptsKey = RedisKeySpace.forLoginAttempts(email.value());

        adapter.recordFailedAttempt(email);
        adapter.recordFailedAttempt(email);
        adapter.recordFailedAttempt(email);

        assertThat(adapter.findActiveLockout(email)).isPresent();

        adapter.clearAttempts(email);

        assertThat(adapter.findActiveLockout(email)).isEmpty();
        assertThat(redisTemplate.opsForValue().get(attemptsKey)).isNull();
    }
}
