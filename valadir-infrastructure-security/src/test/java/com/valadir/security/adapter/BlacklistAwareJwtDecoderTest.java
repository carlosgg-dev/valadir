package com.valadir.security.adapter;

import com.valadir.application.port.out.AccessTokenBlacklist;
import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BlacklistAwareJwtDecoderTest {

    @Mock
    private JwtDecoder delegate;

    @Mock
    private AccessTokenBlacklist accessTokenBlacklist;

    @InjectMocks
    private BlacklistAwareJwtDecoder decoder;

    @Test
    void decode_validNonRevokedToken_returnsJwt() {

        String jti = UUID.randomUUID().toString();
        Jwt jwt = buildJwt(jti);
        given(delegate.decode("token")).willReturn(jwt);
        given(accessTokenBlacklist.isRevoked(jti)).willReturn(false);

        Jwt result = decoder.decode("token");

        assertThat(result).isEqualTo(jwt);
    }

    @Test
    void decode_revokedToken_throwsBadJwtException() {

        String jti = UUID.randomUUID().toString();
        given(delegate.decode("token")).willReturn(buildJwt(jti));
        given(accessTokenBlacklist.isRevoked(jti)).willReturn(true);

        assertThatThrownBy(() -> decoder.decode("token"))
            .isInstanceOf(BadJwtException.class);
    }

    @Test
    void decode_blacklistUnavailable_failsOpenAndReturnsJwt() {

        String jti = UUID.randomUUID().toString();
        Jwt jwt = buildJwt(jti);
        given(delegate.decode("token")).willReturn(jwt);
        given(accessTokenBlacklist.isRevoked(jti)).willThrow(new InfrastructureException("Redis unavailable", new RuntimeException()));

        Jwt result = decoder.decode("token");

        assertThat(result).isEqualTo(jwt);
    }

    @Test
    void decode_tokenWithoutJti_skipsBlacklistCheck() {

        Jwt jwt = buildJwt(null);
        given(delegate.decode("token")).willReturn(jwt);

        Jwt result = decoder.decode("token");

        assertThat(result).isEqualTo(jwt);
        then(accessTokenBlacklist).should(never()).isRevoked(any());
    }

    private static Jwt buildJwt(String jti) {

        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("subject")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900))
            .claims(claims -> {
                if (jti != null) {
                    claims.put("jti", jti);
                }
            })
            .build();
    }
}
