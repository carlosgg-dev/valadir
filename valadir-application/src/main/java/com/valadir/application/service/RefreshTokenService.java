package com.valadir.application.service;

import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.RefreshTokenUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.application.result.TokenValidationResult.Invalid;
import com.valadir.application.result.TokenValidationResult.Valid;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.AccountId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class RefreshTokenService implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenStore refreshTokenStore;
    private final AccountRepository accountRepository;
    private final AuthTokenIssuer authTokenIssuer;

    public RefreshTokenService(RefreshTokenStore refreshTokenStore, AccountRepository accountRepository, AuthTokenIssuer authTokenIssuer) {

        this.refreshTokenStore = refreshTokenStore;
        this.accountRepository = accountRepository;
        this.authTokenIssuer = authTokenIssuer;
    }

    @Override
    public AuthTokenResult refresh(RefreshTokenCommand command) {

        return switch (refreshTokenStore.validate(command.refreshToken())) {
            case Valid(AccountId accountId) -> rotateToken(command.refreshToken(), accountId);
            case Invalid ignored -> throw new ApplicationException("Invalid refresh token", ErrorCode.INVALID_TOKEN);
        };
    }

    private AuthTokenResult rotateToken(String oldRefreshToken, AccountId accountId) {

        MDC.put(MdcKeys.ACCOUNT_ID, accountId.value().toString());

        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.AUTHENTICATION_FAILED));

        AuthTokenResult result = authTokenIssuer.issue(accountId, account.getRole());

        boolean rotated = refreshTokenStore.rotate(oldRefreshToken, result.refreshToken(), accountId);
        if (!rotated) {
            log.warn("Stale refresh token detected");
            throw new ApplicationException("Invalid refresh token", ErrorCode.INVALID_TOKEN);
        }

        log.info("Token refresh successful");
        return result;
    }
}
