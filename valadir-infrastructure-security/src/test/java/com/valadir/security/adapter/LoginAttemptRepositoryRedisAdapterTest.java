package com.valadir.security.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.policy.LoginLockoutPolicy;
import com.valadir.domain.policy.LoginLockoutThreshold;
import com.valadir.security.redis.RedisKeySpace;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LoginAttemptRepositoryRedisAdapterTest {

    private static final int LOCKOUT_MIN_FAILURES = 3;

    private static final LoginLockoutPolicy POLICY = new LoginLockoutPolicy(
        Duration.ofHours(1),
        1,
        List.of(new LoginLockoutThreshold(LOCKOUT_MIN_FAILURES, Duration.ofSeconds(30)))
    );

    private static final Email EMAIL = Email.from("bruce.wayne@email.com");

    @Mock
    private RedisOperations<String, String> redisOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void evaluate_activeLockoutTtl_returnsLockedOutWithRemainingTtl() {

        var remaining = Duration.ofSeconds(25);

        given(redisOperations.getExpire(anyString(), any(TimeUnit.class))).willReturn(remaining.toSeconds());

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        assertThat(adapter.evaluate(EMAIL))
            .isInstanceOfSatisfying(LoginAttemptDecision.LockedOut.class,
                                    lockedOut -> assertThat(lockedOut.remaining()).isEqualTo(remaining));
    }

    @Test
    void evaluate_noLockoutWithStoredCount_returnsPolicyDecisionForCount() {

        long noLockoutTtl = -2; // Redis getExpire returns -2 when the lockout key does not exist
        long storedCount = 2;

        given(redisOperations.getExpire(anyString(), any(TimeUnit.class))).willReturn(noLockoutTtl);
        given(redisOperations.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(String.valueOf(storedCount));

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        assertThat(adapter.evaluate(EMAIL)).isEqualTo(POLICY.decideByCount(storedCount));
    }

    @Test
    void evaluate_nullTtlAndNoStoredCount_returnsPolicyDecisionForZero() {

        given(redisOperations.getExpire(anyString(), any(TimeUnit.class))).willReturn(null);
        given(redisOperations.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        assertThat(adapter.evaluate(EMAIL)).isEqualTo(POLICY.decideByCount(0));
    }

    @Test
    void evaluate_nonNumericStoredCount_propagatesNumberFormatException() {

        long noLockoutTtl = -2;

        given(redisOperations.getExpire(anyString(), any(TimeUnit.class))).willReturn(noLockoutTtl);
        given(redisOperations.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("corrupt_count");

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        // Pins the fail-open boundary: corrupted state must surface, not be masked as Allowed
        assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(() -> adapter.evaluate(EMAIL));
    }

    @Test
    void evaluate_redisError_returnsAllowed() {

        var adapter = new LoginAttemptRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), POLICY);

        assertThat(adapter.evaluate(EMAIL)).isInstanceOf(LoginAttemptDecision.Allowed.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordFailedAttempt_nullCount_returnsEmpty() {

        given(redisOperations.execute(any(RedisScript.class), anyList(), anyString())).willReturn(null);
        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        assertThat(adapter.recordFailedAttempt(EMAIL)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordFailedAttempt_reachesLockoutThreshold_persistsLockoutAndReturnsDuration() {

        long lockoutTriggeringCount = LOCKOUT_MIN_FAILURES;
        Duration expectedLockout = POLICY.lockoutFor(lockoutTriggeringCount);

        given(redisOperations.execute(any(RedisScript.class), anyList(), anyString())).willReturn(lockoutTriggeringCount);
        given(redisOperations.opsForValue()).willReturn(valueOperations);

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        assertThat(adapter.recordFailedAttempt(EMAIL)).hasValue(expectedLockout);

        then(valueOperations).should()
            .set(RedisKeySpace.forLoginLockout(EMAIL.value()), RedisKeySpace.LOGIN_LOCKOUT_VALUE, expectedLockout);
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordFailedAttempt_belowLockoutThreshold_returnsEmptyAndPersistsNoLockout() {

        long belowThresholdCount = 1;

        given(redisOperations.execute(any(RedisScript.class), anyList(), anyString())).willReturn(belowThresholdCount);

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, POLICY);

        assertThat(adapter.recordFailedAttempt(EMAIL)).isEmpty();
        then(redisOperations).should(never()).opsForValue();
    }

    @Test
    void recordFailedAttempt_redisError_returnsEmpty() {

        var adapter = new LoginAttemptRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), POLICY);

        assertThat(adapter.recordFailedAttempt(EMAIL)).isEmpty();
    }

    @Test
    void clearAttempts_redisError_doesNotThrow() {

        var adapter = new LoginAttemptRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), POLICY);

        assertThatNoException().isThrownBy(() -> adapter.clearAttempts(EMAIL));
    }
}
