package com.valadir.application.service;

import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.application.result.TokenValidationResult.Invalid;
import com.valadir.application.result.TokenValidationResult.Reused;
import com.valadir.application.result.TokenValidationResult.Valid;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountId;

public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenStore refreshTokenStore;
    private final AuthTokenIssuer authTokenIssuer;

    public RefreshTokenService(RefreshTokenStore refreshTokenStore, AuthTokenIssuer authTokenIssuer) {

        this.refreshTokenStore = refreshTokenStore;
        this.authTokenIssuer = authTokenIssuer;
    }

    @Override
    public AuthTokenResult refresh(RefreshTokenCommand command) {

        return switch (refreshTokenStore.validate(command.refreshToken())) {
            case Valid ignored -> authTokenIssuer.rotate(command.refreshToken());
            case Reused(AccountId accountId) -> {
                refreshTokenStore.deleteAllByAccount(accountId);
                throw new ApplicationException("Refresh token reuse detected", ErrorCode.TOKEN_REUSE_DETECTED);
            }
            case Invalid ignored -> throw new ApplicationException("Invalid refresh token", ErrorCode.INVALID_TOKEN);
        };
    }
}
