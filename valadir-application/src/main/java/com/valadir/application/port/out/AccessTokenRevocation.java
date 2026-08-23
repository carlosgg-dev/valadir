package com.valadir.application.port.out;

import java.time.Instant;

public interface AccessTokenRevocation {

    boolean isRevoked(String jti, String accountId, Instant issuedAt);
}
