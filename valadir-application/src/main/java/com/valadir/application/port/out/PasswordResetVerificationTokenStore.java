package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.time.Duration;
import java.util.Optional;

public interface PasswordResetVerificationTokenStore {

    void save(String verificationToken, AccountId accountId, Duration ttl);

    Optional<AccountId> resolveAccountId(String verificationToken);

    void delete(String verificationToken);
}
