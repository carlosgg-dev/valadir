package com.valadir.application.port.out;

public interface AccessTokenBlacklist {

    void revoke(String jti);

    boolean isRevoked(String jti);
}
