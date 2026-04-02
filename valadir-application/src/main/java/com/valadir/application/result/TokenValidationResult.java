package com.valadir.application.result;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Role;

public sealed interface TokenValidationResult {

    record Valid(AccountId accountId, Role role) implements TokenValidationResult {

    }

    record Invalid() implements TokenValidationResult {

    }

    record Reused(AccountId accountId) implements TokenValidationResult {

    }
}
