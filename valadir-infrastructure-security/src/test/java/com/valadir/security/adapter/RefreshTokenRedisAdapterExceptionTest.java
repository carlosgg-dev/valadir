package com.valadir.security.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRedisAdapterExceptionTest {

    private static final Duration ONE_WEEK = Duration.ofDays(7);

    // Throws RedisConnectionFailureException on any call — avoids varargs stubbing issues
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> connectionFailureTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    // Throws RedisSystemException on any call — simulates command-level Redis errors (e.g. script execution failure)
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> systemErrorTemplate() {

        return mock(RedisTemplate.class, invocationOnMock -> {
            throw new RedisSystemException("ERR command not allowed", null);
        });
    }

    @Mock
    private JwtProperties jwtProperties;

    @Test
    void validate_redisConnectionFailure_throwsInfrastructureException() {

        var adapter = new RefreshTokenRedisAdapter(connectionFailureTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.validate(token))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void validate_redisSystemError_throwsInfrastructureException() {

        var adapter = new RefreshTokenRedisAdapter(systemErrorTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.validate(token))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void save_redisConnectionFailure_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtl()).willReturn(ONE_WEEK);
        var adapter = new RefreshTokenRedisAdapter(connectionFailureTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(token, accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void save_redisSystemError_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtl()).willReturn(ONE_WEEK);
        var adapter = new RefreshTokenRedisAdapter(systemErrorTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(token, accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void rotate_redisConnectionFailure_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtl()).willReturn(ONE_WEEK);
        var adapter = new RefreshTokenRedisAdapter(connectionFailureTemplate(), jwtProperties);
        String oldToken = UUID.randomUUID().toString();
        String newToken = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.rotate(oldToken, newToken, accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void rotate_redisSystemError_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtl()).willReturn(ONE_WEEK);
        var adapter = new RefreshTokenRedisAdapter(systemErrorTemplate(), jwtProperties);
        String oldToken = UUID.randomUUID().toString();
        String newToken = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.rotate(oldToken, newToken, accountId))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }
}
