package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static com.valadir.common.error.ErrorCode.INFRASTRUCTURE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThatThrownBy(() -> adapter.save(accountId, hashedOtp, otpTtl))
            .isInstanceOf(InfrastructureException.class)
            .hasFieldOrPropertyWithValue("errorCode", INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void save_redisSystemError_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(systemErrorTemplate());
        var accountId = AccountId.generate();
        var hashedOtp = "$argon2id$hashedOtp";
        var otpTtl = Duration.ofMinutes(10);

        assertThatThrownBy(() -> adapter.save(accountId, hashedOtp, otpTtl))
            .isInstanceOf(InfrastructureException.class)
            .hasFieldOrPropertyWithValue("errorCode", INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void find_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(connectionFailureTemplate());
        var accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.find(accountId))
            .isInstanceOf(InfrastructureException.class)
            .hasFieldOrPropertyWithValue("errorCode", INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void find_redisSystemError_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(systemErrorTemplate());
        var accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.find(accountId))
            .isInstanceOf(InfrastructureException.class)
            .hasFieldOrPropertyWithValue("errorCode", INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void delete_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(connectionFailureTemplate());
        var accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.delete(accountId))
            .isInstanceOf(InfrastructureException.class)
            .hasFieldOrPropertyWithValue("errorCode", INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void delete_redisSystemError_throwsInfrastructureException() {

        var adapter = new OtpRedisAdapter(systemErrorTemplate());
        var accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.delete(accountId))
            .isInstanceOf(InfrastructureException.class)
            .hasFieldOrPropertyWithValue("errorCode", INFRASTRUCTURE_UNAVAILABLE);
    }
}
