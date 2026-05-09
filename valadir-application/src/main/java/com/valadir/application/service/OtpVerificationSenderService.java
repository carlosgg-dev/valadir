package com.valadir.application.service;

import com.valadir.application.config.EmailVerificationConfig;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public class OtpVerificationSenderService implements OtpVerificationSender {

    private final EmailVerificationPort emailVerificationPort;
    private final OtpStore otpStore;
    private final OtpHasher otpHasher;
    private final EmailVerificationConfig emailVerificationConfig;

    public OtpVerificationSenderService(
        EmailVerificationPort emailVerificationPort,
        OtpStore otpStore,
        OtpHasher otpHasher,
        EmailVerificationConfig emailVerificationConfig
    ) {

        this.emailVerificationPort = emailVerificationPort;
        this.otpStore = otpStore;
        this.otpHasher = otpHasher;
        this.emailVerificationConfig = emailVerificationConfig;
    }

    @Override
    public void send(AccountId accountId, Email email) {

        var plainCode = OtpGenerator.generate();
        String hashedOtp = otpHasher.hash(plainCode);

        otpStore.save(accountId, hashedOtp, emailVerificationConfig.otpTtl());
        emailVerificationPort.sendVerificationCode(email, plainCode);
    }
}
