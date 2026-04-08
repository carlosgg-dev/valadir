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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private static final long REMAINING_TTL = 600L;

    @Test
    void logout_success_invalidatesBothTokens() {

        service.logout(new LogoutCommand(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN));

        then(logoutTokensInvalidator).should().invalidate(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN);
    }

    @Test
    void logout_invalidationFails_throwsApplicationException() {

        var command = new LogoutCommand(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN);
        willThrow(new RuntimeException("Redis down")).given(logoutTokensInvalidator).invalidate(ACCESS_TOKEN_JTI, REMAINING_TTL, REFRESH_TOKEN);

        assertThatThrownBy(() -> service.logout(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REVOCATION_FAILED);
    }
}
