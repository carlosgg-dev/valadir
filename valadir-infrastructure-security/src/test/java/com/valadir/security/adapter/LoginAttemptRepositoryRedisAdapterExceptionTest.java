package com.valadir.security.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.policy.LoginLockoutPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoginAttemptRepositoryRedisAdapterExceptionTest {

    private static final LoginLockoutPolicy EMPTY_POLICY = new LoginLockoutPolicy(Duration.ofHours(1), List.of());
    private static final Email EMAIL = Email.from("bruce.wayne@email.com");

    @Mock
    private RedisOperations<String, String> redisOperations;

    @SuppressWarnings("unchecked")
    private static RedisOperations<String, String> redisErrorTemplate() {

        return (RedisOperations<String, String>) Proxy.newProxyInstance(
            RedisOperations.class.getClassLoader(),
            new Class[]{RedisOperations.class},
            (proxy, method, args) -> {
                throw new DataAccessException("Redis unavailable") {
                };
            }
        );
    }

    @Test
    void findActiveLockout_redisError_returnsEmpty() {

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisErrorTemplate(), EMPTY_POLICY);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
    }

    @Test
    void findActiveLockout_nullTtl_returnsEmpty() {

        given(redisOperations.getExpire(anyString(), any(TimeUnit.class))).willReturn(null);
        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, EMPTY_POLICY);

        assertThat(adapter.findActiveLockout(EMAIL)).isEmpty();
    }

    @Test
    void recordFailedAttempt_redisError_doesNotThrow() {

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisErrorTemplate(), EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.recordFailedAttempt(EMAIL));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordFailedAttempt_nullCount_doesNotThrow() {

        given(redisOperations.execute(any(RedisScript.class), anyList(), anyString())).willReturn(null);
        var adapter = new LoginAttemptRepositoryRedisAdapter(redisOperations, EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.recordFailedAttempt(EMAIL));
    }

    @Test
    void clearAttempts_redisError_doesNotThrow() {

        var adapter = new LoginAttemptRepositoryRedisAdapter(redisErrorTemplate(), EMPTY_POLICY);

        assertThatNoException().isThrownBy(() -> adapter.clearAttempts(EMAIL));
    }
}
