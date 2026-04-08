package com.valadir.application.port.out;

import com.valadir.application.result.TokenValidationResult;
import com.valadir.domain.model.AccountId;

public interface RefreshTokenStore {

    TokenValidationResult validate(String token);

    void save(String token, AccountId accountId);

    boolean rotate(String oldToken, String newToken, AccountId accountId);
}
