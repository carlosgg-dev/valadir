package com.valadir.application.port.out;

import com.valadir.domain.model.AccountId;

import java.time.Duration;

public interface LogoutTokensInvalidator {

    void invalidate(String jti, Duration remainingTtl, String refreshToken, AccountId accountId);
}
