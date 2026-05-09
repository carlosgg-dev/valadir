package com.valadir.application.service;

import com.valadir.application.command.VerifyEmailCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.VerifyEmailUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpStore;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class VerifyEmailService implements VerifyEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyEmailService.class);

    private final AccountRepository accountRepository;
    private final OtpStore otpStore;
    private final OtpHasher otpHasher;

    public VerifyEmailService(AccountRepository accountRepository, OtpStore otpStore, OtpHasher otpHasher) {

        this.accountRepository = accountRepository;
        this.otpStore = otpStore;
        this.otpHasher = otpHasher;
    }

    @Override
    public void verify(VerifyEmailCommand command) {

        var email = new Email(command.email());

        var account = accountRepository.findByEmail(email)
            .filter(Account::isPendingVerification)
            .orElseThrow(this::verifyException);

        MDC.put(MdcKeys.ACCOUNT_ID, account.getId().value().toString());

        otpStore.find(account.getId())
            .filter(hashedOtp -> otpHasher.matches(command.code(), hashedOtp))
            .orElseThrow(this::verifyException);

        accountRepository.activate(account.getId());
        otpStore.delete(account.getId());

        log.info("Email verified successfully");
    }

    private ApplicationException verifyException() {

        return new ApplicationException("Invalid or expired verification code", ErrorCode.INVALID_VERIFICATION_OTP);
    }
}
