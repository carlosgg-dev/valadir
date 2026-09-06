package com.valadir.application.service;

import com.valadir.domain.model.Account;

public interface AccountActivationOtpSender {

    void send(Account account);
}
