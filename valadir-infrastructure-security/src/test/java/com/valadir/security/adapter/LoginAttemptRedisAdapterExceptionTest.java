package com.valadir.security.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginLockoutPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LoginAttemptRedisAdapterExceptionTest {

    private static final LoginLockoutPolicy EMPTY_POLICY = new LoginLockoutPolicy(Duration.ofHours(1), List.of());
    private static final Email EMAIL = new Email("bruce.wayne@email.com");

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> connectionFailureTemplate() {

        return mock(RedisTemplate.class, invocation -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> systemErrorTemplate() {

        return mock(RedisTemplate.class, invocation -> {
            throw new RedisSystemException("ERR command not allowed", null);
        });
    }

    @Test
    void findActiveLockout_redisConnectionFailure_returnsEmpty() {

        var adapter = new LoginAttemptRedisAdapter(connectionFailureTemplate(), EMPTY_POLICY);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
    }

    @Test
    void findActiveLockout_redisSystemError_returnsEmpty() {

        var adapter = new LoginAttemptRedisAdapter(systemErrorTemplate(), EMPTY_POLICY);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
    }

    @Test
    void findActiveLockout_nullTtl_returnsEmpty() {

        given(redisTemplate.getExpire(anyString(), any(TimeUnit.class))).willReturn(null);
        var adapter = new LoginAttemptRedisAdapter(redisTemplate, EMPTY_POLICY);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
    }

    @Test
    void recordFailedAttempt_redisConnectionFailure_doesNotThrow() {

        var adapter = new LoginAttemptRedisAdapter(connectionFailureTemplate(), EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.recordFailedAttempt(EMAIL));
    }

    @Test
    void recordFailedAttempt_redisSystemError_doesNotThrow() {

        var adapter = new LoginAttemptRedisAdapter(systemErrorTemplate(), EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.recordFailedAttempt(EMAIL));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordFailedAttempt_nullCount_doesNotThrow() {

        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).willReturn(null);
        var adapter = new LoginAttemptRedisAdapter(redisTemplate, EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.recordFailedAttempt(EMAIL));
    }

    @Test
    void clearAttempts_redisConnectionFailure_doesNotThrow() {

        var adapter = new LoginAttemptRedisAdapter(connectionFailureTemplate(), EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.clearAttempts(EMAIL));
    }

    @Test
    void clearAttempts_redisSystemError_doesNotThrow() {

        var adapter = new LoginAttemptRedisAdapter(systemErrorTemplate(), EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.clearAttempts(EMAIL));
    }
}
