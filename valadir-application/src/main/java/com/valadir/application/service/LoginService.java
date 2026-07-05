package com.valadir.application.service;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.exception.AccountLockedException;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.LoginUseCase;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.port.out.CaptchaVerifier;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.RefreshTokenRepository;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.policy.LoginAttemptDecision;
import com.valadir.domain.service.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Optional;

public class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final CaptchaVerifier captchaVerifier;
    private final AccountLockedNotifier accountLockedNotifier;

    public LoginService(
        AccountRepository accountRepository,
        PasswordHasher passwordHasher,
        AuthTokenIssuer authTokenIssuer,
        RefreshTokenRepository refreshTokenRepository,
        LoginAttemptRepository loginAttemptRepository,
        CaptchaVerifier captchaVerifier,
        AccountLockedNotifier accountLockedNotifier
    ) {

        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.authTokenIssuer = authTokenIssuer;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.captchaVerifier = captchaVerifier;
        this.accountLockedNotifier = accountLockedNotifier;
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {

        try {
            var email = Email.from(command.email());
            var rawPassword = RawPassword.from(command.password());

            enforceAttemptDecision(email, command.captchaToken());

            Optional<Account> found = accountRepository.findByEmail(email);
            if (found.isEmpty()) {
                passwordHasher.guardTiming(rawPassword);
                // Accumulate and lock just like a real account so the boundary response is
                // uniform; no owner notification here, there is no real user to warn.
                loginAttemptRepository.recordFailedAttempt(email);
                throw new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
            }

            var account = found.get();
            MDC.put(MdcKeys.ACCOUNT_ID, account.getId().value().toString());
            if (!passwordHasher.matches(rawPassword, account.getPassword())) {
                loginAttemptRepository.recordFailedAttempt(email)
                    .ifPresent(lockout -> accountLockedNotifier.notifyAccountLocked(email, lockout));

                throw new ApplicationException("Invalid credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
            }

            if (!account.isActive()) {
                throw new ApplicationException("Account pending activation", ErrorCode.ACCOUNT_PENDING_ACTIVATION);
            }

            loginAttemptRepository.clearAttempts(email);
            AuthTokenResult result = authTokenIssuer.issue(account.getId(), account.getRole());
            refreshTokenRepository.save(result.refreshToken(), account.getId());
            log.info("Login successful");

            return result;

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }

    private void enforceAttemptDecision(Email email, String captchaToken) {

        switch (loginAttemptRepository.evaluate(email)) {
            case LoginAttemptDecision.LockedOut(Duration remaining) -> throw new AccountLockedException(remaining);
            case LoginAttemptDecision.ChallengeRequired() when !captchaVerifier.isValid(captchaToken) ->
                throw new ApplicationException("Captcha verification required", ErrorCode.CAPTCHA_REQUIRED);
            case LoginAttemptDecision.ChallengeRequired() -> {
                // Step-up satisfied: captcha verified, proceed with the login.
            }
            case LoginAttemptDecision.Allowed() -> {
                // Within normal thresholds: proceed without a step-up.
            }
        }
    }
}
