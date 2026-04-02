package com.valadir.application.port.out;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.User;

public interface RegisterPersistence {

    void save(Account account, User user);
}
