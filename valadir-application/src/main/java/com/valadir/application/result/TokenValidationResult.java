package com.valadir.application.result;

import com.valadir.domain.model.AccountId;

public sealed interface TokenValidationResult {

    record Valid(AccountId accountId) implements TokenValidationResult {

    }

    record Invalid() implements TokenValidationResult {

    }
}
