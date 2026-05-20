package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OtpRepositoryRedisAdapterExceptionTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final String HASHED_OTP = "$argon2id$hashedOtp";
    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis error") {
    };

    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> redisErrorTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw REDIS_ERROR;
        });
    }

    @Test
    void save_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(redisErrorTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(ACCOUNT_ID, HASHED_OTP, OTP_TTL))
            .withCause(REDIS_ERROR);
    }

    @Test
    void find_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(redisErrorTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(ACCOUNT_ID))
            .withCause(REDIS_ERROR);
    }

    @Test
    void delete_redisError_throwsInfrastructureException() {

        var adapter = new OtpRepositoryRedisAdapter(redisErrorTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(ACCOUNT_ID))
            .withCause(REDIS_ERROR);
    }
}
