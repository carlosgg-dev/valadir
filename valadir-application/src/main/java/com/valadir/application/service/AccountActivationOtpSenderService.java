package com.valadir.application.service;

import com.valadir.application.config.AccountActivationConfig;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.PlainOtp;

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
    public void send(Account account) {

        var plainOtp = PlainOtp.generate();
        var hashedOtp = otpHasher.hash(plainOtp);
        var ttl = accountActivationConfig.otpTtl();

        otpRepository.save(account.getId(), hashedOtp, ttl);
        var notification = new OtpNotification(account.getEmail(), plainOtp, ttl, account.getLanguage());
        accountActivationNotifier.sendActivationCode(notification);
    }
}
