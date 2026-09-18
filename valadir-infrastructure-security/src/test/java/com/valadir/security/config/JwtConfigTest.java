package com.valadir.security.config;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private static final String EC_ALGORITHM = "EC";
    private static final int EC_KEY_SIZE = 256;
    private static final String SIGNATURE_ALGORITHM = "ES256";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final String SUBJECT = "6f1d1f1e-0c2a-4f0a-9f6d-1b2c3d4e5f60";

    private JwtDecoder decoder;
    private NimbusJwtEncoder encoder;

    @BeforeEach
    void setUp() throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_ALGORITHM);
        generator.initialize(EC_KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        ECKey key = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
            .privateKey((ECPrivateKey) keyPair.getPrivate())
            .build();

        var properties = new JwtProperties(key.toJSONString(), ACCESS_TTL, REFRESH_TTL);

        decoder = new JwtConfig().nimbusJwtDecoder(properties);
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Test
    void nimbusJwtDecoder_tokenExpiredAMomentAgo_refusesIt() {

        // Inside the minute of skew the default validator would allow, which is the minute the
        // blacklist and the account cutoff no longer cover: both keys expire with the token.
        String token = tokenExpiringAt(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void nimbusJwtDecoder_unexpiredToken_decodesIt() {

        String token = tokenExpiringAt(Instant.now().plus(ACCESS_TTL));

        assertThat(decoder.decode(token).getSubject()).isEqualTo(SUBJECT);
    }

    private String tokenExpiringAt(Instant expiresAt) {

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(SUBJECT)
            .issuedAt(expiresAt.minus(ACCESS_TTL))
            .expiresAt(expiresAt)
            .build();

        JwsHeader header = JwsHeader.with(() -> SIGNATURE_ALGORITHM).build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
