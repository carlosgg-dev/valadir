package com.valadir.application.port.out;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public record PasswordResetVerification(
    AccountId accountId,
    Email email) {

    public boolean emailStillBelongsTo(Account account) {

        return email.equals(account.getEmail());
    }
}
