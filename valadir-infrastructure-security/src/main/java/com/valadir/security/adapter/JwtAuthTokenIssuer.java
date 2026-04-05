package com.valadir.security.adapter;

import com.valadir.application.port.out.AuthTokenIssuer;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Role;
import com.valadir.security.config.JwtProperties;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JwtAuthTokenIssuer implements AuthTokenIssuer {

    private static final String ROLE_CLAIM = "role";
    private static final String ALGORITHM = "ES256";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtAuthTokenIssuer(final JwtEncoder jwtEncoder, final JwtProperties jwtProperties) {

        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthTokenResult issue(final AccountId accountId, final Role role) {

        final String accessToken = buildAccessToken(accountId, role);
        final String refreshToken = UUID.randomUUID().toString();

        return new AuthTokenResult(accessToken, refreshToken);
    }

    private String buildAccessToken(final AccountId accountId, final Role role) {

        final Instant now = Instant.now();
        final JwsHeader header = JwsHeader.with(() -> ALGORITHM).build();

        final JwtClaimsSet claims = JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .subject(accountId.value().toString())
            .claim(ROLE_CLAIM, role.name())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(jwtProperties.accessTokenTtlSeconds()))
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
