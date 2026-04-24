package com.valadir.application.service;

import com.valadir.application.config.VerificationConfig;
import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public class OtpVerificationSenderService implements OtpVerificationSender {

    private final EmailVerificationPort emailVerificationPort;
    private final OtpRepository otpRepository;
    private final OtpHasher otpHasher;
    private final VerificationConfig verificationConfig;

    public OtpVerificationSenderService(
        EmailVerificationPort emailVerificationPort,
        OtpRepository otpRepository,
        OtpHasher otpHasher,
        VerificationConfig verificationConfig
    ) {

        this.emailVerificationPort = emailVerificationPort;
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
        this.verificationConfig = verificationConfig;
    }

    @Override
    public void send(AccountId accountId, Email email) {

        var plainCode = OtpGenerator.generate();
        String hashedOtp = otpHasher.hash(plainCode);

        otpRepository.save(accountId, hashedOtp, verificationConfig.otpTtl());
        emailVerificationPort.sendVerificationCode(email, plainCode);
    }
}
