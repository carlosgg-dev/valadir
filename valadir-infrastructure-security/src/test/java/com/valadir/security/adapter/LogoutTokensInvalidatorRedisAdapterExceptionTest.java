package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LogoutTokensInvalidatorRedisAdapterExceptionTest {

    // Throws RedisConnectionFailureException on any call — avoids varargs stubbing issues
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> connectionFailureTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    // Throws RedisSystemException on any call — simulates command-level Redis errors
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> systemErrorTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisSystemException("ERR command not allowed", null);
        });
    }

    @Test
    void invalidate_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new LogoutTokensInvalidatorRedisAdapter(connectionFailureTemplate());
        String jti = UUID.randomUUID().toString();
        long remainingTtlSeconds = 600L;
        String refreshToken = UUID.randomUUID().toString();
        String accountId = AccountId.generate().toString();

        assertThatThrownBy(() -> adapter.invalidate(jti, remainingTtlSeconds, refreshToken, accountId))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void invalidate_redisSystemError_throwsInfrastructureException() {

        var adapter = new LogoutTokensInvalidatorRedisAdapter(systemErrorTemplate());
        String jti = UUID.randomUUID().toString();
        long remainingTtlSeconds = 600L;
        String refreshToken = UUID.randomUUID().toString();
        String accountId = AccountId.generate().toString();

        assertThatThrownBy(() -> adapter.invalidate(jti, remainingTtlSeconds, refreshToken, accountId))
            .isInstanceOf(InfrastructureException.class);
    }
}
