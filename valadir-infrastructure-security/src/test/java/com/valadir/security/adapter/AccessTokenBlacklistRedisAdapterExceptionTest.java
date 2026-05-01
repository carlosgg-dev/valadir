package com.valadir.security.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.isRevoked("some-jti"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }

    @Test
    void isRevoked_redisSystemError_throwsInfrastructureException() {

        given(redisTemplate.hasKey(any())).willThrow(new RedisSystemException("ERR command not allowed", null));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.isRevoked("some-jti"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INFRASTRUCTURE_UNAVAILABLE);
    }
}
