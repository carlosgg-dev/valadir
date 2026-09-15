package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

public interface ChangeEmailPersistence {

    void change(AccountId accountId, Email newEmail);

    void changeReplacing(AccountId abandonedAccountId, AccountId accountId, Email newEmail);
}
