package com.valadir.application.port.out;

import com.valadir.application.result.TokenValidationResult;
import com.valadir.domain.model.AccountId;

public interface RefreshTokenStore {

    TokenValidationResult validate(String token);

    void delete(String token);

    void deleteAllByAccount(AccountId accountId);
}
