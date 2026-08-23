package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

public interface AccountTokensInvalidator {

    void invalidateAll(AccountId accountId);
}
