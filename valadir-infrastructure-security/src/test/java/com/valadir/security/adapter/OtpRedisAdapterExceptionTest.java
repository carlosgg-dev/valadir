package com.valadir.security.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OtpRedisAdapterExceptionTest {

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
    void save_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(connectionFailureTemplate());
        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";
        var otpTtl = Duration.ofMinutes(10);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(accountId, hashedOtp, otpTtl))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void save_redisSystemError_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(systemErrorTemplate());
        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";
        var otpTtl = Duration.ofMinutes(10);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(accountId, hashedOtp, otpTtl))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void find_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(connectionFailureTemplate());
        var accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void find_redisSystemError_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(systemErrorTemplate());
        var accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.find(accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void delete_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(connectionFailureTemplate());
        var accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void delete_redisSystemError_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(systemErrorTemplate());
        var accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }
}
