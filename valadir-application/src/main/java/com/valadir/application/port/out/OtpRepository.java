package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.time.Duration;
import java.util.Optional;

public interface OtpRepository {

    void save(AccountId accountId, String hashedCode, Duration ttl);

    Optional<String> find(AccountId accountId);

    void delete(AccountId accountId);
}
