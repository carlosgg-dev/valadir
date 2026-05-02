package com.valadir.application.port.out;

import java.time.Duration;

public interface LogoutTokensInvalidator {

    void invalidate(String jti, Duration remainingTtl, String refreshToken, String accountId);
}
