package com.valadir.application.service;

import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class InitiatePasswordResetService implements InitiatePasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiatePasswordResetService.class);

    private final AccountRepository accountRepository;
    private final OtpHasher otpHasher;
    private final PasswordResetOtpSender passwordResetOtpSender;

    public InitiatePasswordResetService(
        AccountRepository accountRepository,
        OtpHasher otpHasher,
        PasswordResetOtpSender passwordResetOtpSender
    ) {

        this.accountRepository = accountRepository;
        this.otpHasher = otpHasher;
        this.passwordResetOtpSender = passwordResetOtpSender;
    }

    @Override
    public void initiate(InitiatePasswordResetCommand command) {

        try {
            var email = Email.from(command.email());
            var account = accountRepository.findByEmail(email);

            if (account.isEmpty()) {
                // Prevent timing-based account enumeration: simulate the OTP hashing cost.
                otpHasher.decoyMatch();
                log.warn("Password reset requested for non-existent email");
                return;
            }

            var foundAccount = account.get();
            var foundAccountId = foundAccount.getId();
            MDC.put(MdcKeys.ACCOUNT_ID, foundAccountId.value().toString());

            if (foundAccount.isPendingActivation()) {
                otpHasher.decoyMatch();
                log.warn("Password reset requested for pending activation account");
                return;
            }

            passwordResetOtpSender.send(foundAccountId, email);

            log.info("Password reset OTP sent");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }
}
