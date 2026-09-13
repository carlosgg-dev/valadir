package com.valadir.application.service;

import com.valadir.application.command.UpdateProfileCommand;
import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.UpdateProfileUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.UpdateProfilePersistence;
import com.valadir.application.port.out.UserRepository;
import com.valadir.application.result.ProfileResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class UpdateProfileService implements UpdateProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateProfileService.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UpdateProfilePersistence updateProfilePersistence;

    public UpdateProfileService(
        AccountRepository accountRepository,
        UserRepository userRepository,
        UpdateProfilePersistence updateProfilePersistence
    ) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.updateProfilePersistence = updateProfilePersistence;
    }

    @Override
    public ProfileResult update(UpdateProfileCommand command) {

        try {
            var accountId = AccountId.from(UUID.fromString(command.accountId()));
            var fullName = FullName.from(command.fullName());
            var givenName = GivenName.from(command.givenName());
            var language = Language.from(command.language());

            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR))
                .changeLanguage(language);

            var user = userRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApplicationException("User not found", ErrorCode.DATA_INTEGRITY_ERROR))
                .rename(fullName, givenName);

            updateProfilePersistence.update(account, user);

            log.info("Profile updated");

            return ProfileResultMapper.toResult(account, user);

        } catch (DomainException e) {
            throw ApplicationException.translate(e);
        }
    }
}
