package com.valadir.application.service;

import com.valadir.application.command.LogoutCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private AccessTokenBlacklist accessTokenBlacklist;
    @InjectMocks
    private LogoutService service;

    @Test
    void logout_bothOperationsSucceed_revokesAccessTokenAndDeletesRefreshToken() {

        var accessTokenJti = "access-jti";
        var refreshToken = "refresh-token";

        service.logout(new LogoutCommand(accessTokenJti, refreshToken));

        then(accessTokenBlacklist).should().revoke(accessTokenJti);
        then(refreshTokenStore).should().delete(refreshToken);
    }

    @Test
    void logout_accessTokenRevocationFails_throwsApplicationException() {

        var accessTokenJti = "access-jti";
        var refreshToken = "refresh-token";
        var command = new LogoutCommand(accessTokenJti, refreshToken);

        willThrow(new RuntimeException("Redis down")).given(accessTokenBlacklist).revoke(accessTokenJti);

        assertThatThrownBy(() -> service.logout(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REVOCATION_FAILED);

        then(refreshTokenStore).should(never()).delete(refreshToken);
    }

    @Test
    void logout_refreshTokenDeletionFails_doesNotThrow() {

        var accessTokenJti = "access-jti";
        var refreshToken = "refresh-token";

        willThrow(new RuntimeException("Redis down")).given(refreshTokenStore).delete(refreshToken);

        service.logout(new LogoutCommand(accessTokenJti, refreshToken));

        then(accessTokenBlacklist).should().revoke(accessTokenJti);
    }
}
