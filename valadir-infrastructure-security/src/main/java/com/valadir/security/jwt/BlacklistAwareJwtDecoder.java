package com.valadir.security.jwt;

import com.valadir.application.port.out.AccessTokenBlacklist;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final AccessTokenBlacklist accessTokenBlacklist;

    public BlacklistAwareJwtDecoder(JwtDecoder delegate, AccessTokenBlacklist accessTokenBlacklist) {

        this.delegate = delegate;
        this.accessTokenBlacklist = accessTokenBlacklist;
    }

    @Override
    public Jwt decode(String token) throws JwtException {

        Jwt jwt = delegate.decode(token);
        String jti = jwt.getId();

        // A lookup that cannot run must not let a revoked token through. The InfrastructureException is
        // left to propagate untouched: translating it into a JwtException would answer 401 and hide the
        // outage behind an ordinary authentication failure.
        if (jti != null && accessTokenBlacklist.isRevoked(jti)) {
            throw new BadJwtException("Token has been revoked");
        }

        return jwt;
    }
}
