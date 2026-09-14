package com.valadir.application.service;

import com.valadir.application.command.ChangePasswordCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.ChangePasswordUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.PasswordChangedNotifier;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ChangePasswordService implements ChangePasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordService.class);

    private final AccountRepository accountRepository;
    private final NewPasswordValidator newPasswordValidator;
    private final AccountReauthenticator accountReauthenticator;
    private final PasswordHasher passwordHasher;
    private final AccountTokensInvalidator accountTokensInvalidator;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordChangedNotifier passwordChangedNotifier;

    public ChangePasswordService(
        AccountRepository accountRepository,
        NewPasswordValidator newPasswordValidator,
        AccountReauthenticator accountReauthenticator,
        PasswordHasher passwordHasher,
        AccountTokensInvalidator accountTokensInvalidator,
        LoginAttemptRepository loginAttemptRepository,
        PasswordChangedNotifier passwordChangedNotifier
    ) {

        this.accountRepository = accountRepository;
        this.newPasswordValidator = newPasswordValidator;
        this.accountReauthenticator = accountReauthenticator;
        this.passwordHasher = passwordHasher;
        this.accountTokensInvalidator = accountTokensInvalidator;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordChangedNotifier = passwordChangedNotifier;
    }

    @Override
    public void change(ChangePasswordCommand command) {

        try {
            var accountId = AccountId.from(UUID.fromString(command.accountId()));
            var currentPassword = RawPassword.from(command.currentPassword());
            var newPassword = RawPassword.from(command.newPassword());

            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

            accountReauthenticator.reauthenticate(account, currentPassword);
            newPasswordValidator.validate(account, newPassword);
            var hashedPassword = passwordHasher.hash(newPassword);

            // Sessions first, neither failure swallowed: either failure leaves the old password valid for the retry.
            // Reversed, a 503 would hide a changed password, and retrying with the old one would count as a failure.
            accountTokensInvalidator.invalidateAll(accountId);
            accountRepository.updatePassword(accountId, hashedPassword);

            // The failures were counted against a password that no longer exists
            loginAttemptRepository.clearAttempts(account.getEmail());

            notifyPasswordChangedQuietly(account);

            log.info("Password changed");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }

    private void notifyPasswordChangedQuietly(Account account) {

        // The password is already changed and every session revoked: a 503 would tell the owner the
        // change failed, and their retry with the old password would be counted as a failed attempt
        try {
            passwordChangedNotifier.notifyPasswordChanged(account.getEmail(), account.getLanguage());
        } catch (InfrastructureException e) {
            log.warn("Password changed but the owner notification failed", e);
        }
    }
}
