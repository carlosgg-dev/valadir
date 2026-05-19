package com.valadir.application.service;

import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.PasswordResetOtpStore;
import com.valadir.application.port.out.PasswordResetVerificationTokenStore;
import com.valadir.application.result.PasswordResetOtpVerificationResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class VerifyPasswordResetOtpService implements VerifyPasswordResetOtpUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyPasswordResetOtpService.class);

    private final AccountRepository accountRepository;
    private final PasswordResetOtpStore passwordResetOtpStore;
    private final OtpHasher otpHasher;
    private final PasswordResetVerificationTokenStore passwordResetVerificationTokenStore;
    private final PasswordResetConfig passwordResetConfig;

    public VerifyPasswordResetOtpService(
        AccountRepository accountRepository,
        PasswordResetOtpStore passwordResetOtpStore,
        OtpHasher otpHasher,
        PasswordResetVerificationTokenStore passwordResetVerificationTokenStore,
        PasswordResetConfig passwordResetConfig
    ) {

        this.accountRepository = accountRepository;
        this.passwordResetOtpStore = passwordResetOtpStore;
        this.otpHasher = otpHasher;
        this.passwordResetVerificationTokenStore = passwordResetVerificationTokenStore;
        this.passwordResetConfig = passwordResetConfig;
    }

    @Override
    public PasswordResetOtpVerificationResult verify(VerifyPasswordResetOtpCommand command) {

        var email = new Email(command.email());
        var foundAccount = getAccount(email);

        AccountId foundAccountId = foundAccount.getId();
        MDC.put(MdcKeys.ACCOUNT_ID, foundAccountId.value().toString());

        var hashedOtp = passwordResetOtpStore.find(foundAccountId)
            .orElseThrow(this::applicationException);

        if (!otpHasher.matches(command.code(), hashedOtp)) {
            throw applicationException();
        }

        passwordResetOtpStore.delete(foundAccountId);

        var verificationToken = UUID.randomUUID().toString();
        passwordResetVerificationTokenStore.save(verificationToken, foundAccountId, passwordResetConfig.verificationTokenTtl());

        log.info("Password reset OTP verified, verification token issued");

        return new PasswordResetOtpVerificationResult(verificationToken);
    }

    private Account getAccount(Email email) {

        var account = accountRepository.findByEmail(email);

        if (account.isEmpty()) {
            // Prevent timing-based account enumeration: simulate the OTP hashing cost.
            otpHasher.guardTiming();
            throw applicationException();
        }

        return account.get();
    }

    private ApplicationException applicationException() {

        return new ApplicationException("Invalid or expired password reset code", ErrorCode.INVALID_PASSWORD_RESET_OTP);
    }
}
