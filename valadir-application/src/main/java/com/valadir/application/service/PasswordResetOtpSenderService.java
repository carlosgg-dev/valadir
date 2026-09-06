package com.valadir.application.service;

import com.valadir.application.config.PasswordResetConfig;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.domain.model.Account;
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
    public void send(Account account) {

        var plainOtp = PlainOtp.generate();
        var hashedOtp = otpHasher.hash(plainOtp);
        var ttl = passwordResetConfig.otpTtl();

        otpRepository.save(account.getId(), hashedOtp, ttl);
        var notification = new OtpNotification(account.getEmail(), plainOtp, ttl, account.getLanguage());
        passwordResetNotifier.sendResetCode(notification);
    }
}
