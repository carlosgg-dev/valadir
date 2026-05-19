package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByAccountId(AccountId accountId);
}
