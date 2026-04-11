package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.common.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(BlacklistAwareJwtDecoder.class);

    private final JwtDecoder delegate;
    private final AccessTokenBlacklist accessTokenBlacklist;

    public BlacklistAwareJwtDecoder(final JwtDecoder delegate, final AccessTokenBlacklist accessTokenBlacklist) {

        this.delegate = delegate;
        this.accessTokenBlacklist = accessTokenBlacklist;
    }

    @Override
    public Jwt decode(final String token) throws JwtException {

        final Jwt jwt = delegate.decode(token);
        final String jti = jwt.getId();

        if (jti != null) {
            try {
                if (accessTokenBlacklist.isRevoked(jti)) {
                    throw new BadJwtException("Token has been revoked");
                }
            } catch (InfrastructureException e) {
                log.warn("Blacklist check unavailable, failing open: {}", e.getMessage());
            }
        }

        return jwt;
    }
}
