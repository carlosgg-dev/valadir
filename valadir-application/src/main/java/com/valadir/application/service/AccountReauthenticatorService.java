package com.valadir.application.service;

import com.valadir.application.exception.AccountLockedException;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.service.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class AccountReauthenticatorService implements AccountReauthenticator {

    private static final Logger log = LoggerFactory.getLogger(AccountReauthenticatorService.class);

    private final PasswordHasher passwordHasher;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AccountLockedNotifier accountLockedNotifier;

    public AccountReauthenticatorService(
        PasswordHasher passwordHasher,
        LoginAttemptRepository loginAttemptRepository,
        AccountLockedNotifier accountLockedNotifier
    ) {

        this.passwordHasher = passwordHasher;
        this.loginAttemptRepository = loginAttemptRepository;
        this.accountLockedNotifier = accountLockedNotifier;
    }

    // The password is all that stands between a stolen access token and what an authenticated caller is
    // about to do to the account, so guesses count against the login's counter: one password, one budget,
    // and a lockout reached at any door closes all of them. No CAPTCHA step-up — it tells people from bots
    // at the anonymous door, and this caller already holds a session; the lockout tiers bound the guesses alike.
    @Override
    public void reauthenticate(Account account, RawPassword password) {

        if (loginAttemptRepository.decisionFor(account.getEmail()) instanceof LoginAttemptDecision.LockedOut(Duration remaining)) {
            throw new AccountLockedException(remaining);
        }

        if (!passwordHasher.matches(password, account.getHashedPassword())) {
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
