package com.valadir.application.port.out;

public interface AccessTokenBlacklist {

    void revoke(String jti, long remainingTtlSeconds);

    boolean isRevoked(String jti);
}
