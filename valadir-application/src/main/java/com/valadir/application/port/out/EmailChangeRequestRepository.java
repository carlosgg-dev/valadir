package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.time.Duration;
import java.util.Optional;

public interface EmailChangeRequestRepository {

    void save(AccountId accountId, EmailChangeRequest request, Duration ttl);

    Optional<EmailChangeRequest> find(AccountId accountId);

    void delete(AccountId accountId);
}
