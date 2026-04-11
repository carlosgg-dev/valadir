package com.valadir.application.port.out;

public interface AccessTokenBlacklist {

    boolean isRevoked(String jti);
}
