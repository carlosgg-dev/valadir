package com.valadir.application.service;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.port.out.AccountActivationPort;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public class AccountActivationOtpSenderService implements AccountActivationOtpSender {

    private final AccountActivationPort accountActivationPort;
    private final OtpStore otpStore;
    private final OtpHasher otpHasher;
    private final AccountActivationConfig accountActivationConfig;

    public AccountActivationOtpSenderService(
        AccountActivationPort accountActivationPort,
        OtpStore otpStore,
        OtpHasher otpHasher,
        AccountActivationConfig accountActivationConfig
    ) {

        this.accountActivationPort = accountActivationPort;
        this.otpStore = otpStore;
        this.otpHasher = otpHasher;
        this.accountActivationConfig = accountActivationConfig;
    }

    @Override
    public void send(AccountId accountId, Email email) {

        var plainCode = OtpGenerator.generate();
        String hashedOtp = otpHasher.hash(plainCode);

        otpStore.save(accountId, hashedOtp, accountActivationConfig.otpTtl());
        accountActivationPort.sendActivationCode(email, plainCode);
    }
}
