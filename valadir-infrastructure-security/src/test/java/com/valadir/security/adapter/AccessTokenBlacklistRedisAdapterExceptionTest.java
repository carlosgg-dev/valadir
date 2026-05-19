package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistRedisAdapterExceptionTest {

    private static final DataAccessException REDIS_ERROR = new DataAccessException("Redis error") {
    };

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AccessTokenBlacklistRedisAdapter adapter;

    @Test
    void isRevoked_redisError_throwsInfrastructureException() {

        given(redisTemplate.hasKey(any())).willThrow(REDIS_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.isRevoked("some-jti"))
            .withCause(REDIS_ERROR);
    }
}
