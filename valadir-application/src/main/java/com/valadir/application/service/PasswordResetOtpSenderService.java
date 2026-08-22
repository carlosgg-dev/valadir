package com.valadir.application.service;

import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;

public class PasswordResetOtpSenderService implements PasswordResetOtpSender {

    private final PasswordResetNotifier passwordResetNotifier;
    private final OtpRepository otpRepository;
    private final OtpHasher otpHasher;
    private final PasswordResetConfig passwordResetConfig;

    public PasswordResetOtpSenderService(
        PasswordResetNotifier passwordResetNotifier,
        OtpRepository otpRepository,
        OtpHasher otpHasher,
        PasswordResetConfig passwordResetConfig
    ) {

        this.passwordResetNotifier = passwordResetNotifier;
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
        this.passwordResetConfig = passwordResetConfig;
    }

    @Override
    public void send(AccountId accountId, Email email) {

        var plainOtp = PlainOtp.generate();
        var hashedOtp = otpHasher.hash(plainOtp);

        otpRepository.save(accountId, hashedOtp, passwordResetConfig.otpTtl());
        passwordResetNotifier.sendResetCode(email, plainOtp);
    }
}
