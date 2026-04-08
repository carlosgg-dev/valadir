package com.valadir.application.port.out;

public interface LogoutTokensInvalidator {

    void invalidate(String jti, long remainingTtlSeconds, String refreshToken);
}
