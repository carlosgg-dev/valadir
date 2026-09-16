package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<AccountId> accountIdFor(String token);

    void save(String token, AccountId accountId);

    boolean rotate(String oldToken, String newToken, AccountId accountId);
}
