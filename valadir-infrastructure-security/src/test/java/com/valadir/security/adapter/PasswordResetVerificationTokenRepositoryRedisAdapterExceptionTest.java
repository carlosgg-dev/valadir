package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class PasswordResetVerificationTokenRepositoryRedisAdapterExceptionTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final String TOKEN = "some-verification-token";

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis down") {
    };

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PasswordResetVerificationTokenRepositoryRedisAdapter adapter;

    @Test
    void save_redisUnavailable_throwsInfrastructureException() {

        var tokenTtl = Duration.ofMinutes(10);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(REDIS_ERROR).given(valueOperations).set(any(), any(), any(Duration.class));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(TOKEN, ACCOUNT_ID, tokenTtl))
            .withCause(REDIS_ERROR);
    }

    @Test
    void resolveAccountId_redisUnavailable_throwsInfrastructureException() {

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(any())).willThrow(REDIS_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.resolveAccountId(TOKEN))
            .withCause(REDIS_ERROR);
    }

    @Test
    void delete_redisUnavailable_throwsInfrastructureException() {

        given(redisTemplate.delete(anyString())).willThrow(REDIS_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(TOKEN))
            .withCause(REDIS_ERROR);
    }
}
