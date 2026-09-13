package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

public interface DeleteAccountPersistence {

    void delete(AccountId accountId);
}
