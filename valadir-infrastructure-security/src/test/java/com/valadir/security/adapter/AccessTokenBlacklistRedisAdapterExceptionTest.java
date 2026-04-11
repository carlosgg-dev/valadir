package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistRedisAdapterExceptionTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AccessTokenBlacklistRedisAdapter adapter;

    @Test
    void revoke_redisUnavailable_throwsInfrastructureException() {

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("connection refused")).given(valueOperations).set(any(), any(), any());

        assertThatThrownBy(() -> adapter.revoke("some-jti", 900L))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void isRevoked_redisUnavailable_throwsInfrastructureException() {

        given(redisTemplate.hasKey(any())).willThrow(new RedisConnectionFailureException("connection refused"));

        assertThatThrownBy(() -> adapter.isRevoked("some-jti"))
            .isInstanceOf(InfrastructureException.class);
    }
}
