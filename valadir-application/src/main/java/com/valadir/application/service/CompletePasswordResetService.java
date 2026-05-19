package com.valadir.application.service;

import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.PasswordResetVerificationTokenStore;
import com.valadir.application.port.out.RefreshTokenStore;
import com.valadir.application.port.out.UserRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.mdc.MdcKeys;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.UserProfileData;
import com.valadir.domain.service.PasswordHasher;
import com.valadir.domain.service.PasswordSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CompletePasswordResetService implements CompletePasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompletePasswordResetService.class);

    private final PasswordResetVerificationTokenStore passwordResetVerificationTokenStore;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordSecurityService passwordSecurityService;
    private final RefreshTokenStore refreshTokenStore;

    public CompletePasswordResetService(
        PasswordResetVerificationTokenStore passwordResetVerificationTokenStore,
        AccountRepository accountRepository,
        UserRepository userRepository,
        PasswordHasher passwordHasher,
        PasswordSecurityService passwordSecurityService,
        RefreshTokenStore refreshTokenStore
    ) {

        this.passwordResetVerificationTokenStore = passwordResetVerificationTokenStore;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.passwordSecurityService = passwordSecurityService;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Override
    public void complete(CompletePasswordResetCommand command) {

        var accountId = passwordResetVerificationTokenStore.resolveAccountId(command.verificationToken())
            .orElseThrow(() -> new ApplicationException("Invalid or expired password reset verification", ErrorCode.INVALID_PASSWORD_RESET_VERIFICATION_TOKEN));

        MDC.put(MdcKeys.ACCOUNT_ID, accountId.value().toString());

        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

        var user = userRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ApplicationException("User not found", ErrorCode.DATA_INTEGRITY_ERROR));

        var rawPassword = new RawPassword(command.newPassword());
        var profileData = new UserProfileData(user.getFullName(), user.getGivenName());
        passwordSecurityService.validatePassword(rawPassword, account.getEmail(), profileData);

        var hashedPassword = passwordHasher.hash(rawPassword);
        accountRepository.updatePassword(accountId, hashedPassword);
        passwordResetVerificationTokenStore.delete(command.verificationToken());
        refreshTokenStore.revokeAllForAccount(accountId);

        log.info("Password reset completed, all sessions revoked");
    }
}
