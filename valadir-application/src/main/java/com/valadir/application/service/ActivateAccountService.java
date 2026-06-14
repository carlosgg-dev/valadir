package com.valadir.application.service;

import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
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

        try {
            var email = Email.from(command.email());
            var plainOtp = PlainOtp.from(command.plainOtp());

            var account = accountRepository.findByEmail(email)
                .filter(Account::isPendingActivation)
                .orElseThrow(this::applicationException);

            MDC.put(MdcKeys.ACCOUNT_ID, account.getId().value().toString());

            otpRepository.find(account.getId())
                .filter(hashedOtp -> otpHasher.matches(plainOtp, hashedOtp))
                .orElseThrow(this::applicationException);

            accountRepository.activate(account.getId());

            deleteOtp(account.getId());

            log.info("Account activated successfully");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }

    private ApplicationException applicationException() {

        return new ApplicationException("Invalid or expired account activation OTP", ErrorCode.INVALID_ACCOUNT_ACTIVATION_OTP);
    }

    private void deleteOtp(AccountId accountId) {

        // Redis cleanup is best-effort: account activation is the critical operation.
        // Failure leaves a stale OTP that cannot be reused, since the account is no longer pending activation.
        try {
            otpRepository.delete(accountId);
        } catch (InfrastructureException e) {
            log.warn("Account activated but OTP Redis cleanup failed — OTP will expire via TTL", e);
        }
    }
}
