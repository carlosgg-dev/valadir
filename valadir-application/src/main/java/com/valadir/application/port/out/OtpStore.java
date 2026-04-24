package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.time.Duration;
import java.util.Optional;

public interface OtpStore {

    void save(AccountId accountId, String hashedOtp, Duration ttl);

    Optional<String> find(AccountId accountId);

    void delete(AccountId accountId);
}
