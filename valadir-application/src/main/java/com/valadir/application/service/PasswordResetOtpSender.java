package com.valadir.application.service;

import com.valadir.domain.model.Account;

public interface PasswordResetOtpSender {

    void send(Account account);
}
