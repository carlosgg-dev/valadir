package com.valadir.application.service;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.otp.PlainOtp;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public class AccountActivationOtpSenderService implements AccountActivationOtpSender {

    private final AccountActivationNotifier accountActivationNotifier;
    private final OtpRepository otpRepository;
    private final OtpHasher otpHasher;
    private final AccountActivationConfig accountActivationConfig;

    public AccountActivationOtpSenderService(
        AccountActivationNotifier accountActivationNotifier,
        OtpRepository otpRepository,
        OtpHasher otpHasher,
        AccountActivationConfig accountActivationConfig
    ) {

        this.accountActivationNotifier = accountActivationNotifier;
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
        this.accountActivationConfig = accountActivationConfig;
    }

    @Override
    public void send(AccountId accountId, Email email) {

        var plainOtp = PlainOtp.generate();
        var hashedOtp = otpHasher.hash(plainOtp);

        otpRepository.save(accountId, hashedOtp, accountActivationConfig.otpTtl());
        accountActivationNotifier.sendActivationCode(email, plainOtp);
    }
}
