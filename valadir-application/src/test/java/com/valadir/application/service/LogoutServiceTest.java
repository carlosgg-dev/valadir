package com.valadir.application.service;

import com.valadir.application.command.LogoutCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    private static final String ACCESS_TOKEN_JTI = "access-jti";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final Duration REMAINING_TTL = Duration.ofMinutes(10);

    private static final InfrastructureException INFRA_ERROR = new InfrastructureException("Redis error");

    @Mock
    private LogoutTokensInvalidator logoutTokensInvalidator;

    @InjectMocks
    private LogoutService service;

    @Test
    void logout_success_invalidatesBothTokens() {

        var accountId = AccountId.generate();

        service.logout(new LogoutCommand(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, accountId));

        then(logoutTokensInvalidator).should().invalidate(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, accountId);
    }

    @Test
    void logout_invalidationFails_throwsApplicationException() {

        var accountId = AccountId.generate();

        var command = new LogoutCommand(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, accountId);

        willThrow(INFRA_ERROR)
            .given(logoutTokensInvalidator).invalidate(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, accountId);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.logout(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REVOCATION_FAILED)
            .withCause(INFRA_ERROR);
    }
}
