package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRedisAdapterExceptionTest {

    // Returns RedisConnectionFailureException on any call — avoids varargs stubbing issues
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, String> failingTemplate() {

        return mock(RedisTemplate.class, inv -> {
            throw new RedisConnectionFailureException("connection refused");
        });
    }

    @Mock
    private JwtProperties jwtProperties;

    @Test
    void validate_redisUnavailable_throwsInfrastructureException() {

        final var adapter = new RefreshTokenRedisAdapter(failingTemplate(), jwtProperties);
        final String token = UUID.randomUUID().toString();

        assertThatThrownBy(() -> adapter.validate(token))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void save_redisUnavailable_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtlSeconds()).willReturn(604800L);
        final var adapter = new RefreshTokenRedisAdapter(failingTemplate(), jwtProperties);
        final String token = UUID.randomUUID().toString();
        final AccountId accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.save(token, accountId))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void rotate_redisUnavailable_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtlSeconds()).willReturn(604800L);
        final var adapter = new RefreshTokenRedisAdapter(failingTemplate(), jwtProperties);
        final String oldToken = UUID.randomUUID().toString();
        final String newToken = UUID.randomUUID().toString();
        final AccountId accountId = AccountId.generate();

        assertThatThrownBy(() -> adapter.rotate(oldToken, newToken, accountId))
            .isInstanceOf(InfrastructureException.class);
    }
}
