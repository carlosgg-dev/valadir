package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class LogoutTokensInvalidatorRedisAdapterExceptionTest {

    @Test
    void invalidate_redisError_throwsInfrastructureException() {

        var jti = UUID.randomUUID().toString();
        var remainingTtl = Duration.ofMinutes(10);
        var refreshToken = UUID.randomUUID().toString();
        var accountId = AccountId.generate();

        var adapter = new LogoutTokensInvalidatorRedisAdapter(RedisTestUtils.errorTemplate());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.invalidate(jti, remainingTtl, refreshToken, accountId))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
