package com.valadir.application.service;

import com.valadir.application.command.LogoutCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.common.error.ErrorCode;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class LogoutService implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutService.class);

    private final LogoutTokensInvalidator logoutTokensInvalidator;

    public LogoutService(LogoutTokensInvalidator logoutTokensInvalidator) {

        this.logoutTokensInvalidator = logoutTokensInvalidator;
    }

    @Override
    public void logout(LogoutCommand command) {

        var accountId = AccountId.from(UUID.fromString(command.accountId()));

        try {
            logoutTokensInvalidator.invalidate(
                command.accessTokenJti(),
                command.accessTokenRemainingTtl(),
                command.refreshToken(),
                accountId
            );
            log.info("Logout successful");
        } catch (InfrastructureException e) {
            throw new ApplicationException("Logout failed", ErrorCode.TOKEN_REVOCATION_FAILED, e);
        }
    }
}
