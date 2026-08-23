package com.valadir.security.jwt;

import com.valadir.application.port.out.AccessTokenRevocation;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

public class RevocationAwareJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final AccessTokenRevocation accessTokenRevocation;

    public RevocationAwareJwtDecoder(JwtDecoder delegate, AccessTokenRevocation accessTokenRevocation) {

        this.delegate = delegate;
        this.accessTokenRevocation = accessTokenRevocation;
    }

    @Override
    public Jwt decode(String token) throws JwtException {

        Jwt jwt = delegate.decode(token);
        String jti = jwt.getId();
        String accountId = jwt.getSubject();
        Instant issuedAt = jwt.getIssuedAt();

        // A token missing any of these was not issued here, and letting it through would skip the
        // revocation check altogether.
        if (jti == null || accountId == null || issuedAt == null) {
            throw new BadJwtException("Token cannot be checked against revocation");
        }

        // A lookup that cannot run must not let a revoked token through. The InfrastructureException is
        // left to propagate untouched: translating it into a JwtException would answer 401 and hide the
        // outage behind an ordinary authentication failure.
        if (accessTokenRevocation.isRevoked(jti, accountId, issuedAt)) {
            throw new BadJwtException("Token has been revoked");
        }

        return jwt;
    }
}
