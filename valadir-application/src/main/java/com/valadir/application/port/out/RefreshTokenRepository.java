package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<AccountId> validate(String token);

    void save(String token, AccountId accountId);

    boolean rotate(String oldToken, String newToken, AccountId accountId);

    void revokeAllForAccount(AccountId accountId);
}
