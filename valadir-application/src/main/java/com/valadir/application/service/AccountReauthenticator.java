package com.valadir.application.service;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.RawPassword;

public interface AccountReauthenticator {

    void reauthenticate(Account account, RawPassword password);
}
