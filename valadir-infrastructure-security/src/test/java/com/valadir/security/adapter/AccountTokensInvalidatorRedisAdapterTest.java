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

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountTokensInvalidatorRedisAdapterTest {

    @Mock
    private JwtProperties jwtProperties;

    @Test
    void invalidateAll_redisError_throwsInfrastructureException() {

        var accountId = AccountId.generate();
        given(jwtProperties.accessTokenTtl()).willReturn(Duration.ofMinutes(15));
        var adapter = new AccountTokensInvalidatorRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard(), jwtProperties);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.invalidateAll(accountId))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
