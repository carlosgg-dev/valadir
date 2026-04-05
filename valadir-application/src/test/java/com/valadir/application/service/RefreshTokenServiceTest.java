package com.valadir.application.service;

import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.application.result.TokenValidationResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    private AccountRepository accountRepository;
    @Mock
    private AuthTokenIssuer authTokenIssuer;
    @InjectMocks
    private RefreshTokenService service;

    private final AccountId accountId = AccountId.generate();
    private final Account account = Account.reconstitute(
        accountId,
        new Email("bruce.wayne@email.com"),
        new HashedPassword("$2a$12$hashed"),
        Role.USER
    );

    @Test
    void refresh_validToken_deletesOldAndSavesNewToken() {

        var oldRefreshToken = "old-refresh-token";
        var newAccessToken = "new-access";
        var newRefreshToken = "new-refresh";
        var expectedRefreshResult = new AuthTokenResult(newAccessToken, newRefreshToken);
        var validToken = new TokenValidationResult.Valid(accountId);

        given(refreshTokenStore.validate(oldRefreshToken)).willReturn(validToken);
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(authTokenIssuer.issue(accountId, Role.USER)).willReturn(expectedRefreshResult);

        var result = service.refresh(new RefreshTokenCommand(oldRefreshToken));

        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        then(refreshTokenStore).should().delete(oldRefreshToken);
        then(refreshTokenStore).should().save(newRefreshToken, accountId);
    }

    @Test
    void refresh_validToken_accountNotFound_throwsApplicationException() {

        var oldRefreshToken = "old-refresh-token";
        var command = new RefreshTokenCommand(oldRefreshToken);
        var validToken = new TokenValidationResult.Valid(accountId);

        given(refreshTokenStore.validate(oldRefreshToken)).willReturn(validToken);
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);

        then(authTokenIssuer).should(never()).issue(any(), any());
        then(refreshTokenStore).should(never()).save(any(), any());
    }

    @Test
    void refresh_invalidToken_throwsApplicationException() {

        var oldRefreshToken = "old-refresh-token";
        var command = new RefreshTokenCommand(oldRefreshToken);
        var invalidToken = new TokenValidationResult.Invalid();

        given(refreshTokenStore.validate(oldRefreshToken)).willReturn(invalidToken);

        assertThatThrownBy(() -> service.refresh(command))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        then(refreshTokenStore).should(never()).delete(any());
        then(authTokenIssuer).should(never()).issue(any(), any());
    }
}
