package com.valadir.application.service;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.UserRepository;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.service.PasswordSecurityService;

public class NewPasswordValidatorService implements NewPasswordValidator {

    private final UserRepository userRepository;
    private final PasswordSecurityService passwordSecurityService;

    public NewPasswordValidatorService(UserRepository userRepository, PasswordSecurityService passwordSecurityService) {

        this.userRepository = userRepository;
        this.passwordSecurityService = passwordSecurityService;
    }

    @Override
    public void validate(Account account, RawPassword newPassword) {

        var user = userRepository.findByAccountId(account.getId())
            .orElseThrow(() -> new ApplicationException("User not found", ErrorCode.DATA_INTEGRITY_ERROR));

        passwordSecurityService.validatePassword(newPassword, account.getEmail(), user);
    }
}
