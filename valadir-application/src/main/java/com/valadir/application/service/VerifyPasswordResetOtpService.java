package com.valadir.application.service;

import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.PasswordResetOtpRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenRepository;
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
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final OtpHasher otpHasher;
    private final PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository;
    private final PasswordResetConfig passwordResetConfig;

    public VerifyPasswordResetOtpService(
        AccountRepository accountRepository,
        PasswordResetOtpRepository passwordResetOtpRepository,
        OtpHasher otpHasher,
        PasswordResetVerificationTokenRepository passwordResetVerificationTokenRepository,
        PasswordResetConfig passwordResetConfig
    ) {

        this.accountRepository = accountRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.otpHasher = otpHasher;
        this.passwordResetVerificationTokenRepository = passwordResetVerificationTokenRepository;
        this.passwordResetConfig = passwordResetConfig;
    }

    @Override
    public PasswordResetOtpVerificationResult verify(VerifyPasswordResetOtpCommand command) {

        var email = new Email(command.email());
        var foundAccount = getAccount(email);

        AccountId foundAccountId = foundAccount.getId();
        MDC.put(MdcKeys.ACCOUNT_ID, foundAccountId.value().toString());

        var hashedOtp = passwordResetOtpRepository.find(foundAccountId)
            .orElseThrow(this::applicationException);

        if (!otpHasher.matches(command.code(), hashedOtp)) {
            throw applicationException();
        }

        passwordResetOtpRepository.delete(foundAccountId);

        var verificationToken = UUID.randomUUID().toString();
        passwordResetVerificationTokenRepository.save(verificationToken, foundAccountId, passwordResetConfig.verificationTokenTtl());

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
