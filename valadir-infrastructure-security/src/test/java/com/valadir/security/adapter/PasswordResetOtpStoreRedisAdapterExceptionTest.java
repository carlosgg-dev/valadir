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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class PasswordResetOtpStoreRedisAdapterExceptionTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PasswordResetOtpStoreRedisAdapter adapter;

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis down") {
    };

    @Test
    void save_redisUnavailable_throwsInfrastructureException() {

        var otpTtl = Duration.ofMinutes(15);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(REDIS_ERROR).given(valueOperations).set(any(), any(), any(Duration.class));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT_ID, "$argon2id$hash", otpTtl))
            .withCause(REDIS_ERROR);
    }

    @Test
    void find_redisUnavailable_throwsInfrastructureException() {

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(any())).willThrow(REDIS_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(ACCOUNT_ID))
            .withCause(REDIS_ERROR);
    }

    @Test
    void delete_redisUnavailable_throwsInfrastructureException() {

        given(redisTemplate.delete(any(String.class))).willThrow(REDIS_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(ACCOUNT_ID))
            .withCause(REDIS_ERROR);
    }
}
