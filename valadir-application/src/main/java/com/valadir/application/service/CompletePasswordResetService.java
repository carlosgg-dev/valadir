package com.valadir.application.service;

import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.AccountTokensInvalidator;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CompletePasswordResetService implements CompletePasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompletePasswordResetService.class);

    private final PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository;
    private final AccountRepository accountRepository;
    private final NewPasswordValidator newPasswordValidator;
    private final PasswordHasher passwordHasher;
    private final AccountTokensInvalidator accountTokensInvalidator;
    private final LoginAttemptRepository loginAttemptRepository;

    public CompletePasswordResetService(
        PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository,
        AccountRepository accountRepository,
        NewPasswordValidator newPasswordValidator,
        PasswordHasher passwordHasher,
        AccountTokensInvalidator accountTokensInvalidator,
        LoginAttemptRepository loginAttemptRepository
    ) {

        this.passwordResetVerificationTokenRepository = passwordResetVerificationTokenRepository;
        this.accountRepository = accountRepository;
        this.newPasswordValidator = newPasswordValidator;
        this.passwordHasher = passwordHasher;
        this.accountTokensInvalidator = accountTokensInvalidator;
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Override
    public void complete(CompletePasswordResetCommand command) {

        try {
            var accountId = passwordResetVerificationTokenRepository.resolveAccountId(command.verificationToken())
                .orElseThrow(() -> new ApplicationException("Invalid or expired password reset verification", ErrorCode.INVALID_PASSWORD_RESET_VERIFICATION_TOKEN));

            MDC.put(MdcKeys.ACCOUNT_ID, accountId.value().toString());

            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

            var newPassword = RawPassword.from(command.newPassword());
            newPasswordValidator.validate(account, newPassword);
            var hashedPassword = passwordHasher.hash(newPassword);

            // Nothing swallowed: a 204 over live sessions would defeat the reset
            accountTokensInvalidator.invalidateAll(accountId);
            accountRepository.updatePassword(accountId, hashedPassword);

            // Unconditional: reading the counter first would buy nothing, since a reset without prior
            // failures deletes nothing. Where there were failures, they were counted against a password
            // that no longer exists, and the tier would outlive the reset that made them irrelevant.
            loginAttemptRepository.clearAttempts(account.getEmail());

            // Last: an earlier failure leaves the token valid for the retry
            passwordResetVerificationTokenRepository.delete(command.verificationToken());

            log.info("Password reset completed");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }
}
