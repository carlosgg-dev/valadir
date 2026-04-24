package com.valadir.application.service;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public interface OtpVerificationSender {

    void send(AccountId accountId, Email email);
}
