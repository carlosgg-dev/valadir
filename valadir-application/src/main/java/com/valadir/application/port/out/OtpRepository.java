package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.HashedOtp;

import java.time.Duration;
import java.util.Optional;

public interface OtpRepository {

    void save(AccountId accountId, HashedOtp hashedOtp, Duration ttl);

    Optional<HashedOtp> find(AccountId accountId);

    void delete(AccountId accountId);
}
