package com.valadir.security.adapter;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.valadir.application.result.AuthTokenResult;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Role;
import com.valadir.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthTokenIssuerTest {

    private static final long ACCESS_TTL = 900L;
    private static final long REFRESH_TTL = 604800L;
    private static final String EC_ALGORITHM = "EC";
    private static final int EC_KEY_SIZE = 256;

    private JwtAuthTokenIssuer issuer;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(EC_ALGORITHM);
        kpg.initialize(EC_KEY_SIZE);
        KeyPair keyPair = kpg.generateKeyPair();

        ECKey ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
            .privateKey((ECPrivateKey) keyPair.getPrivate())
            .build();

        var properties = new JwtProperties(ecKey.toJSONString(), ACCESS_TTL, REFRESH_TTL);
        var jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(ecKey)));
        issuer = new JwtAuthTokenIssuer(jwtEncoder, properties);

        var jwkSource = new ImmutableJWKSet<>(new JWKSet(ecKey.toPublicJWK()));
        var processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource));
        jwtDecoder = new NimbusJwtDecoder(processor);
    }

    @Test
    void issue_validInput_returnsAccessTokenWithCorrectClaims() {

        var accountId = AccountId.generate();

        AuthTokenResult result = issuer.issue(accountId, Role.USER);
        var jwt = jwtDecoder.decode(result.accessToken());

        assertThat(jwt.getId()).isNotNull();
        assertThat(jwt.getSubject()).isEqualTo(accountId.value().toString());
        assertThat(jwt.getClaim("role").toString()).hasToString(Role.USER.name());
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
    }

    @Test
    void issue_validInput_accessTokenExpiresAfterTtl() {

        AuthTokenResult result = issuer.issue(AccountId.generate(), Role.USER);
        var jwt = jwtDecoder.decode(result.accessToken());

        long issuedAt = requireNonNull(jwt.getIssuedAt()).getEpochSecond();
        long expiresAt = requireNonNull(jwt.getExpiresAt()).getEpochSecond();
        long ttl = expiresAt - issuedAt;

        assertThat(ttl).isEqualTo(ACCESS_TTL);
    }

    @Test
    void issue_validInput_refreshTokenIsOpaqueUuid() {

        AuthTokenResult result = issuer.issue(AccountId.generate(), Role.USER);

        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).doesNotContain(".");
    }

    @Test
    void issue_calledTwice_generatesDifferentJtis() {

        var accountId = AccountId.generate();

        String token1 = issuer.issue(accountId, Role.USER).accessToken();
        String token2 = issuer.issue(accountId, Role.USER).accessToken();

        assertThat(jwtDecoder.decode(token1).getId())
            .isNotEqualTo(jwtDecoder.decode(token2).getId());
    }
}
