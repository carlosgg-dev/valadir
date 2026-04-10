package com.valadir.application.service;

import com.valadir.application.command.LogoutCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.LogoutUseCase;
import com.valadir.application.port.out.LogoutTokensInvalidator;
import com.valadir.common.error.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutService implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutService.class);

    private final LogoutTokensInvalidator logoutTokensInvalidator;

    public LogoutService(final LogoutTokensInvalidator logoutTokensInvalidator) {

        this.logoutTokensInvalidator = logoutTokensInvalidator;
    }

    @Override
    public void logout(final LogoutCommand command) {

        try {
            logoutTokensInvalidator.invalidate(
                command.accessTokenJti(),
                command.accessTokenRemainingTtlSeconds(),
                command.refreshToken(),
                command.accountId()
            );
            log.info("Logout successful");
        } catch (Exception e) {
            throw new ApplicationException("Logout failed", ErrorCode.TOKEN_REVOCATION_FAILED, e);
        }
    }
}
