package com.valadir.application.service;

import com.valadir.application.command.DeleteAccountCommand;
import com.valadir.application.exception.AccountLockedException;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.DeleteAccountUseCase;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.DeleteAccountPersistence;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.service.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

public class DeleteAccountService implements DeleteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccountService.class);

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AccountLockedNotifier accountLockedNotifier;
    private final AccountTokensInvalidator accountTokensInvalidator;
    private final DeleteAccountPersistence deleteAccountPersistence;

    public DeleteAccountService(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        LoginAttemptRepository loginAttemptRepository,
        AccountLockedNotifier accountLockedNotifier,
        AccountTokensInvalidator accountTokensInvalidator,
        DeleteAccountPersistence deleteAccountPersistence
    ) {

        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.loginAttemptRepository = loginAttemptRepository;
        this.accountLockedNotifier = accountLockedNotifier;
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

            reauthenticate(account, rawPassword);

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

    // The password is all that stands between a stolen access token and an irreversible deletion, so
    // guesses count against the login's counter: one password, one budget, and a lockout reached at
    // either door closes both. No CAPTCHA step-up — it tells people from bots at the anonymous door,
    // and this caller already holds a session; the lockout tiers bound the guesses alike.
    private void reauthenticate(Account account, RawPassword rawPassword) {

        if (loginAttemptRepository.evaluate(account.getEmail()) instanceof LoginAttemptDecision.LockedOut(Duration remaining)) {
            throw new AccountLockedException(remaining);
        }

        if (!passwordHasher.matches(rawPassword, account.getPassword())) {
            loginAttemptRepository.recordFailedAttempt(account.getEmail())
                .ifPresent(lockout -> notifyAccountLockedQuietly(account, lockout));

            throw new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
        }
    }

    private void notifyAccountLockedQuietly(Account account, Duration lockout) {

        // The lockout is already applied: turning the 401 into a 503 would only tell an attacker exactly
        // when the threshold was crossed
        try {
            accountLockedNotifier.notifyAccountLocked(account.getEmail(), lockout, account.getLanguage());
        } catch (InfrastructureException e) {
            log.warn("Account locked but the owner notification failed", e);
        }
    }
}
