package com.valadir.security.adapter;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRedisAdapterExceptionTest {

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

        assertThatThrownBy(() -> adapter.validate(token))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void validate_redisSystemError_throwsInfrastructureException() {

        var adapter = new RefreshTokenRedisAdapter(systemErrorTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();

        assertThatThrownBy(() -> adapter.validate(token))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void save_redisConnectionFailure_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtlSeconds()).willReturn(604800L);
        var adapter = new RefreshTokenRedisAdapter(connectionFailureTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.save(token, accountId))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void save_redisSystemError_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtlSeconds()).willReturn(604800L);
        var adapter = new RefreshTokenRedisAdapter(systemErrorTemplate(), jwtProperties);
        String token = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.save(token, accountId))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void rotate_redisConnectionFailure_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtlSeconds()).willReturn(604800L);
        var adapter = new RefreshTokenRedisAdapter(connectionFailureTemplate(), jwtProperties);
        String oldToken = UUID.randomUUID().toString();
        String newToken = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.rotate(oldToken, newToken, accountId))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void rotate_redisSystemError_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtlSeconds()).willReturn(604800L);
        var adapter = new RefreshTokenRedisAdapter(systemErrorTemplate(), jwtProperties);
        String oldToken = UUID.randomUUID().toString();
        String newToken = UUID.randomUUID().toString();
        AccountId accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.rotate(oldToken, newToken, accountId))
            .isInstanceOf(InfrastructureException.class);
    }
}
