package com.valadir.application.service;

import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.application.result.TokenValidationResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private AuthTokenIssuer authTokenIssuer;
    @InjectMocks
    private RefreshTokenService service;

    private final AccountId accountId = AccountId.generate();

    @Test
    void shouldRefresh_WhenTokenIsValid() {

        var validToken = "valid-token";
        var newAccessToken = "new-access";
        var newRefreshToken = "new-refresh";

        given(refreshTokenStore.validate(validToken)).willReturn(new TokenValidationResult.Valid(accountId, Role.USER));
        given(authTokenIssuer.rotate(validToken)).willReturn(new AuthTokenResult(newAccessToken, newRefreshToken));

        var result = service.refresh(new RefreshTokenCommand(validToken));

        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        then(authTokenIssuer).should().rotate(validToken);
    }

    @Test
    void shouldRevokeAllSessions_WhenTokenIsReused() {

        var reusedToken = "reused-token";
        var command = new RefreshTokenCommand(reusedToken);

        given(refreshTokenStore.validate(reusedToken)).willReturn(new TokenValidationResult.Reused(accountId));

        assertThatThrownBy(() -> service.refresh(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_REUSE_DETECTED);

        then(refreshTokenStore).should().deleteAllByAccount(accountId);
        then(authTokenIssuer).should(never()).rotate(any());
    }

    @Test
    void shouldThrowException_WhenTokenIsInvalid() {

        var invalidToken = "invalid-token";
        var command = new RefreshTokenCommand(invalidToken);

        given(refreshTokenStore.validate(invalidToken)).willReturn(new TokenValidationResult.Invalid());

        assertThatThrownBy(() -> service.refresh(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        then(refreshTokenStore).should(never()).delete(any());
        then(authTokenIssuer).should(never()).rotate(any());
    }
}
