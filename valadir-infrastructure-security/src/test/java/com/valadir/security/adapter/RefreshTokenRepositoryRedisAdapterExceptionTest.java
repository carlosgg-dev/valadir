package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.security.config.JwtProperties;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryRedisAdapterExceptionTest {

    private static final Duration ONE_WEEK = Duration.ofDays(7);
    private static final String OLD_TOKEN = UUID.randomUUID().toString();
    private static final String NEW_TOKEN = UUID.randomUUID().toString();
    private static final AccountId ACCOUNT_ID = AccountId.generate();

    @Mock
    private JwtProperties jwtProperties;

    @Test
    void validate_redisError_throwsInfrastructureException() {

        var adapter = new RefreshTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), jwtProperties);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.validate(NEW_TOKEN))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void save_redisError_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtl()).willReturn(ONE_WEEK);
        var adapter = new RefreshTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), jwtProperties);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(NEW_TOKEN, ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void rotate_redisError_throwsInfrastructureException() {

        given(jwtProperties.refreshTokenTtl()).willReturn(ONE_WEEK);
        var adapter = new RefreshTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), jwtProperties);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.rotate(OLD_TOKEN, NEW_TOKEN, ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void revokeAllForAccount_redisError_throwsInfrastructureException() {

        var adapter = new RefreshTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), jwtProperties);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.revokeAllForAccount(ACCOUNT_ID))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
