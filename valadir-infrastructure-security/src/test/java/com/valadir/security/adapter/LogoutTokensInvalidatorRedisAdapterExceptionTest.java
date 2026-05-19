package com.valadir.security.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LogoutTokensInvalidatorRedisAdapterExceptionTest {

    private static final String JTI = UUID.randomUUID().toString();
    private static final Duration REMAINING_TTL = Duration.ofMinutes(10);
    private static final String REFRESH_TOKEN = UUID.randomUUID().toString();
    private static final String ACCOUNT_ID = AccountId.generate().toString();

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis error") {
    };

    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> redisErrorTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw REDIS_ERROR;
        });
    }

    @Test
    void invalidate_redisError_throwsInfrastructureException() {

        var adapter = new LogoutTokensInvalidatorRedisAdapter(redisErrorTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.invalidate(JTI, REMAINING_TTL, REFRESH_TOKEN, ACCOUNT_ID))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE)
            .withCause(REDIS_ERROR);
    }
}
