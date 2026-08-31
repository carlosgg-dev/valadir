package com.valadir.security.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PasswordResetVerificationTokenRepositoryRedisAdapterTest {

    private static final AccountId ACCOUNT_ID = AccountId.generate();
    private static final String TOKEN = "some-verification-token";

    @Test
    void save_redisUnavailable_throwsInfrastructureException() {

        var tokenTtl = Duration.ofMinutes(10);
        var adapter = new PasswordResetVerificationTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(TOKEN, ACCOUNT_ID, tokenTtl))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void resolveAccountId_redisUnavailable_throwsInfrastructureException() {

        var adapter = new PasswordResetVerificationTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.resolveAccountId(TOKEN))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void delete_redisUnavailable_throwsInfrastructureException() {

        var adapter = new PasswordResetVerificationTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(TOKEN))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
