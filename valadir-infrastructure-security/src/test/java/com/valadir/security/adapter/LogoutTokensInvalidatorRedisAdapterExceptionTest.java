package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LogoutTokensInvalidatorRedisAdapterExceptionTest {

    // Returns RedisConnectionFailureException on any call — avoids varargs stubbing issues
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> failingTemplate() {

        return mock(RedisTemplate.class, inv -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    @Test
    void invalidate_redisUnavailable_throwsInfrastructureException() {

        final var adapter = new LogoutTokensInvalidatorRedisAdapter(failingTemplate());
        String jti = UUID.randomUUID().toString();
        long remainingTtlSeconds = 600L;
        String refreshToken = UUID.randomUUID().toString();
        String accountId = AccountId.generate().toString();

        assertThatThrownBy(() -> adapter.invalidate(jti, remainingTtlSeconds, refreshToken, accountId))
            .isInstanceOf(InfrastructureException.class);
    }
}
