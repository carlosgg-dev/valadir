package com.valadir.application.port.out;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.User;

public interface UpdateProfilePersistence {

    void update(Account account, User user);
}
