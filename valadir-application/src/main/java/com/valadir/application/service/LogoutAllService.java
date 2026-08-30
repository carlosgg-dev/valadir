package com.valadir.application.service;

import com.valadir.application.command.LogoutAllCommand;
import com.valadir.application.port.in.LogoutAllUseCase;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.domain.model.AccountId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class LogoutAllService implements LogoutAllUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutAllService.class);

    private final AccountTokensInvalidator accountTokensInvalidator;

    public LogoutAllService(AccountTokensInvalidator accountTokensInvalidator) {

        this.accountTokensInvalidator = accountTokensInvalidator;
    }

    // No catch, unlike the password reset: there this call is cleanup after the password already
    // changed; here it is the operation, and an outage must surface as 503, not as a 204 that
    // closed nothing.
    @Override
    public void logoutAll(LogoutAllCommand command) {

        var accountId = AccountId.from(UUID.fromString(command.accountId()));

        accountTokensInvalidator.invalidateAll(accountId);

        log.info("Logout from every session successful");
    }
}
