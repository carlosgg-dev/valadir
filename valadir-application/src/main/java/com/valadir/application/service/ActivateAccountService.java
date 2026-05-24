package com.valadir.application.service;

import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ActivateAccountService implements ActivateAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivateAccountService.class);

    private final AccountRepository accountRepository;
    private final OtpRepository otpRepository;
    private final OtpHasher otpHasher;

    public ActivateAccountService(AccountRepository accountRepository, OtpRepository otpRepository, OtpHasher otpHasher) {

        this.accountRepository = accountRepository;
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
    }

    @Override
    public void activate(ActivateAccountCommand command) {

        var account = accountRepository.findByEmail(command.email())
            .filter(Account::isPendingActivation)
            .orElseThrow(this::applicationException);

        MDC.put(MdcKeys.ACCOUNT_ID, account.getId().value().toString());

        otpRepository.find(account.getId())
            .filter(hashedOtp -> otpHasher.matches(command.plainOtp(), hashedOtp))
            .orElseThrow(this::applicationException);

        accountRepository.activate(account.getId());
        otpRepository.delete(account.getId());

        log.info("Account activated successfully");
    }

    private ApplicationException applicationException() {

        return new ApplicationException("Invalid or expired account activation OTP", ErrorCode.INVALID_ACCOUNT_ACTIVATION_OTP);
    }
}
