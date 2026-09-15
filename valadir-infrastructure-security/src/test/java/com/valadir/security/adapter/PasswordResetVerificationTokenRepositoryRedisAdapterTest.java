package com.valadir.security.adapter;

import com.valadir.application.port.out.PasswordResetVerification;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.test.redis.RedisTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

import static com.valadir.security.redis.CircuitGuards.buildClosedCircuitGuard;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PasswordResetVerificationTokenRepositoryRedisAdapterTest {

    private static final String TOKEN = "some-verification-token";

    private final PasswordResetVerificationTokenRepositoryRedisAdapter adapter =
        new PasswordResetVerificationTokenRepositoryRedisAdapter(RedisTestUtils.errorTemplate(), buildClosedCircuitGuard());

    @Test
    void save_redisUnavailable_throwsInfrastructureException() {

        var verification = new PasswordResetVerification(AccountId.generate(), Email.from("bruce.wayne@email.com"));
        var tokenTtl = Duration.ofMinutes(10);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.save(TOKEN, verification, tokenTtl))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void verificationFor_redisUnavailable_throwsInfrastructureException() {

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.verificationFor(TOKEN))
            .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void delete_redisUnavailable_throwsInfrastructureException() {

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.delete(TOKEN))
            .withCauseInstanceOf(DataAccessException.class);
    }
}
