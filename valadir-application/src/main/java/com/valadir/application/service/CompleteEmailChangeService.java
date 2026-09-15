package com.valadir.application.service;

import com.valadir.application.command.CompleteEmailChangeCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.CompleteEmailChangeUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.ChangeEmailPersistence;
import com.valadir.application.port.out.EmailChangeRequestRepository;
import com.valadir.application.port.out.EmailChangedNotifier;
import com.valadir.application.port.out.LoginAttemptRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.PlainOtp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CompleteEmailChangeService implements CompleteEmailChangeUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompleteEmailChangeService.class);

    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final OtpHasher otpHasher;
    private final AccountRepository accountRepository;
    private final EmailHolderResolver emailHolderResolver;
    private final ChangeEmailPersistence changeEmailPersistence;
    private final LoginAttemptRepository loginAttemptRepository;
    private final EmailChangedNotifier emailChangedNotifier;

    public CompleteEmailChangeService(
        EmailChangeRequestRepository emailChangeRequestRepository,
        OtpHasher otpHasher,
        AccountRepository accountRepository,
        EmailHolderResolver emailHolderResolver,
        ChangeEmailPersistence changeEmailPersistence,
        LoginAttemptRepository loginAttemptRepository,
        EmailChangedNotifier emailChangedNotifier
    ) {

        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.otpHasher = otpHasher;
        this.accountRepository = accountRepository;
        this.emailHolderResolver = emailHolderResolver;
        this.changeEmailPersistence = changeEmailPersistence;
        this.loginAttemptRepository = loginAttemptRepository;
        this.emailChangedNotifier = emailChangedNotifier;
    }

    @Override
    public void complete(CompleteEmailChangeCommand command) {

        try {
            var accountId = AccountId.from(UUID.fromString(command.accountId()));
            var code = PlainOtp.from(command.code());

            var request = emailChangeRequestRepository.find(accountId)
                .filter(pending -> otpHasher.matches(code, pending.hashedOtp()))
                .orElseThrow(() -> new ApplicationException("Invalid or expired email change OTP", ErrorCode.INVALID_EMAIL_CHANGE_OTP));

            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

            // Resolved again: the address was free at initiation, but another account may have activated it since
            var newEmail = request.newEmail();
            emailHolderResolver.replaceableHolderFor(newEmail).ifPresentOrElse(
                abandonedAccountId -> changeEmailPersistence.changeReplacing(abandonedAccountId, accountId, newEmail),
                () -> changeEmailPersistence.change(accountId, newEmail)
            );

            deleteRequestQuietly(accountId);

            // Otherwise a new account on the old address would inherit the failures counted against this one
            loginAttemptRepository.clearAttempts(account.getEmail());

            notifyEmailChangedQuietly(account);

            log.info("Email changed");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }

    private void deleteRequestQuietly(AccountId accountId) {

        // The email is already changed. A request left behind cannot change it twice: the address it carries now
        // belongs to this very account, so replaying the code resolves as a conflict until the TTL removes it.
        try {
            emailChangeRequestRepository.delete(accountId);
        } catch (InfrastructureException e) {
            log.warn("Email changed but the change request cleanup failed — it will expire via TTL", e);
        }
    }

    private void notifyEmailChangedQuietly(Account account) {

        // The change is applied: a 503 would tell the owner it failed, and their retry would find the code already spent
        try {
            emailChangedNotifier.notifyEmailChanged(account.getEmail(), account.getLanguage());
        } catch (InfrastructureException e) {
            log.warn("Email changed but the notification to the previous address failed", e);
        }
    }
}
