package com.valadir.application.service;

import com.valadir.application.command.LogoutCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.common.error.ErrorCode;
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

    @Mock
    private LogoutTokensInvalidator logoutTokensInvalidator;
    @InjectMocks
    private LogoutService service;

    private static final String ACCESS_TOKEN_JTI = "access-jti";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String ACCOUNT_ID = "account-uuid";
    private static final Duration REMAINING_TTL = Duration.ofMinutes(10);

    @Test
    void logout_success_invalidatesBothTokens() {

        service.logout(new LogoutCommand(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, ACCOUNT_ID));

        then(logoutTokensInvalidator).should().invalidate(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, ACCOUNT_ID);
    }

    @Test
    void logout_invalidationFails_throwsApplicationException() {

        var command = new LogoutCommand(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, ACCOUNT_ID);
        willThrow(new RuntimeException("Redis down"))
            .given(logoutTokensInvalidator).invalidate(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN, ACCOUNT_ID);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> service.logout(command))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REVOCATION_FAILED);
    }
}
