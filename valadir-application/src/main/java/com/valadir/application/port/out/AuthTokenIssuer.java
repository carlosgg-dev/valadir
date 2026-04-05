package com.valadir.application.port.out;

import com.valadir.application.result.AuthTokenResult;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Role;

public interface AuthTokenIssuer {

    AuthTokenResult issue(AccountId accountId, Role role);
}
