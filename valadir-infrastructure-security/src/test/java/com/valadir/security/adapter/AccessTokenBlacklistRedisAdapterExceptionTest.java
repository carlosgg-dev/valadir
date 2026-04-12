package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistRedisAdapterExceptionTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AccessTokenBlacklistRedisAdapter adapter;

    @Test
    void isRevoked_redisConnectionFailure_throwsInfrastructureException() {

        given(redisTemplate.hasKey(any())).willThrow(new RedisConnectionFailureException("connection refused"));

        assertThatThrownBy(() -> adapter.isRevoked("some-jti"))
            .isInstanceOf(InfrastructureException.class);
    }

    @Test
    void isRevoked_redisSystemError_throwsInfrastructureException() {

        given(redisTemplate.hasKey(any())).willThrow(new RedisSystemException("ERR command not allowed", null));

        assertThatThrownBy(() -> adapter.isRevoked("some-jti"))
            .isInstanceOf(InfrastructureException.class);
    }
}
