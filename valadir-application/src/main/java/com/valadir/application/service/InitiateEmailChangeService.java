package com.valadir.application.service;

import com.valadir.application.command.InitiateEmailChangeCommand;
import com.valadir.application.config.EmailChangeConfig;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.InitiateEmailChangeUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.EmailChangeNotifier;
import com.valadir.application.port.out.EmailChangeRequest;
import com.valadir.application.port.out.EmailChangeRequestRepository;
import com.valadir.application.port.out.OtpHasher;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import com.valadir.domain.model.RawPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class InitiateEmailChangeService implements InitiateEmailChangeUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiateEmailChangeService.class);

    private final AccountRepository accountRepository;
    private final AccountReauthenticator accountReauthenticator;
    private final EmailHolderResolver emailHolderResolver;
    private final OtpHasher otpHasher;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final EmailChangeNotifier emailChangeNotifier;
    private final EmailChangeConfig emailChangeConfig;

    public InitiateEmailChangeService(
        AccountRepository accountRepository,
        AccountReauthenticator accountReauthenticator,
        EmailHolderResolver emailHolderResolver,
        OtpHasher otpHasher,
        EmailChangeRequestRepository emailChangeRequestRepository,
        EmailChangeNotifier emailChangeNotifier,
        EmailChangeConfig emailChangeConfig
    ) {

        this.accountRepository = accountRepository;
        this.accountReauthenticator = accountReauthenticator;
        this.emailHolderResolver = emailHolderResolver;
        this.otpHasher = otpHasher;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.emailChangeNotifier = emailChangeNotifier;
        this.emailChangeConfig = emailChangeConfig;
    }

    @Override
    public void initiate(InitiateEmailChangeCommand command) {

        try {
            var accountId = AccountId.from(UUID.fromString(command.accountId()));
            var newEmail = Email.from(command.newEmail());
            var password = RawPassword.from(command.password());

            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

            accountReauthenticator.reauthenticate(account, password);

            // Only the conflict matters yet: a pending holder is replaced at completion, once the address is proved
            emailHolderResolver.replaceableHolderFor(newEmail);

            var plainOtp = PlainOtp.generate();
            var ttl = emailChangeConfig.otpTtl();

            // Keyed by account: a second request overwrites the first, so only the latest code is ever valid
            emailChangeRequestRepository.save(accountId, new EmailChangeRequest(newEmail, otpHasher.hash(plainOtp)), ttl);
            emailChangeNotifier.sendConfirmationCode(new OtpNotification(newEmail, plainOtp, ttl, account.getLanguage()));

            log.info("Email change initiated, confirmation code sent to the new address");

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }
}
