package com.valadir.application.service;

import com.valadir.application.command.VerifyEmailCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.VerifyEmailUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyEmailService implements VerifyEmailUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyEmailService.class);

    private final AccountRepository accountRepository;
    private final OtpRepository otpRepository;
    private final OtpHasher otpHasher;

    public VerifyEmailService(
        AccountRepository accountRepository,
        OtpRepository otpRepository,
        OtpHasher otpHasher
    ) {

        this.accountRepository = accountRepository;
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
    }

    @Override
    public void verify(VerifyEmailCommand command) {

        var email = new Email(command.email());

        var account = accountRepository.findByEmail(email)
            .filter(found -> found.getStatus() == AccountStatus.PENDING_VERIFICATION)
            .orElseThrow(this::verifyException);

        otpRepository.find(account.getId())
            .filter(hashedOtp -> otpHasher.matches(command.code(), hashedOtp))
            .orElseThrow(this::verifyException);

        accountRepository.save(account.activate());
        otpRepository.delete(account.getId());

        log.info("Email verified successfully [account={}]", account.getId().value());
    }

    private ApplicationException verifyException() {

        return new ApplicationException("Invalid or expired verification code", ErrorCode.INVALID_VERIFICATION_OTP);
    }
}
