package com.valadir.application.service;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.in.GetProfileUseCase;
import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.UserRepository;
import com.valadir.application.result.ProfileResult;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountId;

import java.util.UUID;

public class GetProfileService implements GetProfileUseCase {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public GetProfileService(AccountRepository accountRepository, UserRepository userRepository) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProfileResult getProfile(String accountId) {

        var id = AccountId.from(UUID.fromString(accountId));

        var account = accountRepository.findById(id)
            .orElseThrow(() -> new ApplicationException("Account not found", ErrorCode.DATA_INTEGRITY_ERROR));

        var user = userRepository.findByAccountId(id)
            .orElseThrow(() -> new ApplicationException("User not found", ErrorCode.DATA_INTEGRITY_ERROR));

        return ProfileResultMapper.toResult(account, user);
    }
}
