package com.valadir.application.port.out;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findByEmail(Email email);

    void save(Account account);
}
