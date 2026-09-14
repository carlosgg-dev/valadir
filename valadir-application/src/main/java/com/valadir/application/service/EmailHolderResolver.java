package com.valadir.application.service;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;

import java.util.Optional;

public interface EmailHolderResolver {

    Optional<AccountId> replaceableHolderFor(Email email);
}
