package com.valadir.application.service;

import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.application.port.out.PasswordResetOtpRepository;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class InitiatePasswordResetService implements InitiatePasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiatePasswordResetService.class);

    private final AccountRepository accountRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final OtpHasher otpHasher;
    private final PasswordResetNotifier passwordResetNotifier;
    private final PasswordResetConfig passwordResetConfig;

    public InitiatePasswordResetService(
        AccountRepository accountRepository,
        PasswordResetOtpRepository passwordResetOtpRepository,
        OtpHasher otpHasher,
        PasswordResetNotifier passwordResetNotifier,
        PasswordResetConfig passwordResetConfig
    ) {

        this.accountRepository = accountRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.otpHasher = otpHasher;
        this.passwordResetNotifier = passwordResetNotifier;
        this.passwordResetConfig = passwordResetConfig;
    }

    @Override
    public void initiate(InitiatePasswordResetCommand command) {

        var email = new Email(command.email());
        var account = accountRepository.findByEmail(email);

        if (account.isEmpty()) {
            // Prevent timing-based account enumeration: simulate the OTP hashing cost.
            otpHasher.guardTiming();
            log.warn("Password reset requested for non-existent email");
            return;
        }

        var foundAccount = account.get();
        var foundAccountId = foundAccount.getId();
        MDC.put(MdcKeys.ACCOUNT_ID, foundAccountId.value().toString());

        if (foundAccount.isPendingActivation()) {
            otpHasher.guardTiming();
            log.warn("Password reset requested for pending activation account");
            return;
        }

        var plainCode = OtpGenerator.generate();
        var hashedOtp = otpHasher.hash(plainCode);

        passwordResetOtpRepository.save(foundAccountId, hashedOtp, passwordResetConfig.otpTtl());
        passwordResetNotifier.sendResetCode(email, plainCode);

        log.info("Password reset code sent");
    }
}
