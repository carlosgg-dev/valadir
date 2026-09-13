package com.valadir.application.service;

import com.valadir.application.command.DeleteAccountCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.DeleteAccountUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.DeleteAccountPersistence;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.RawPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DeleteAccountService implements DeleteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccountService.class);

    private final AccountRepository accountRepository;
    private final AccountReauthenticator accountReauthenticator;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AccountTokensInvalidator accountTokensInvalidator;
    private final DeleteAccountPersistence deleteAccountPersistence;

    public DeleteAccountService(
        AccountRepository accountRepository,
        AccountReauthenticator accountReauthenticator,
        LoginAttemptRepository loginAttemptRepository,
        AccountTokensInvalidator accountTokensInvalidator,
        DeleteAccountPersistence deleteAccountPersistence
    ) {

        this.accountRepository = accountRepository;
        this.accountReauthenticator = accountReauthenticator;
        this.loginAttemptRepository = loginAttemptRepository;
        this.accountTokensInvalidator = accountTokensInvalidator;
        this.deleteAccountPersistence = deleteAccountPersistence;
    }

    @Override
    public void delete(DeleteAccountCommand command) {

        try {
            var accountId = AccountId.from(UUID.fromString(command.accountId()));
            var rawPassword = RawPassword.from(command.password());

            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

            accountReauthenticator.reauthenticate(account, rawPassword);

            // Sessions before rows, and neither failure swallowed. A deletion that fails after the
            // revocation leaves an account the owner signs back into and deletes again; the reverse
            // order would leave live sessions on an account that no longer exists, out of any retry's reach.
            accountTokensInvalidator.invalidateAll(accountId);
            deleteAccountPersistence.delete(accountId);

            // Otherwise a new account on the same email would inherit the failures counted above
            loginAttemptRepository.clearAttempts(account.getEmail());

            log.info("Account deleted");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }
}
