package com.valadir.application.service;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.RawPassword;

public interface NewPasswordValidator {

    void validate(Account account, RawPassword newPassword);
}
