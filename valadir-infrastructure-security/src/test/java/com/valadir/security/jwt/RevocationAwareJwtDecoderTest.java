package com.valadir.security.jwt;

import com.valadir.application.port.out.AccessTokenRevocation;
import com.valadir.common.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RevocationAwareJwtDecoderTest {

    private static final String TOKEN = "token";
    private static final String JTI = UUID.randomUUID().toString();
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();
    private static final Instant ISSUED_AT = Instant.now();

    // The three claims the revocation check reads. Every token this system issues carries them.
    private static final Map<String, Object> REVOCABLE_CLAIMS = Map.of(
        JwtClaimNames.JTI, JTI,
        JwtClaimNames.SUB, ACCOUNT_ID,
        JwtClaimNames.IAT, ISSUED_AT
    );

    @Mock
    private JwtDecoder delegate;

    @Mock
    private AccessTokenRevocation accessTokenRevocation;

    @InjectMocks
    private RevocationAwareJwtDecoder decoder;

    @Test
    void decode_validNonRevokedToken_returnsJwt() {

        Jwt jwt = buildJwt();
        given(delegate.decode(TOKEN)).willReturn(jwt);
        given(accessTokenRevocation.isRevoked(JTI, ACCOUNT_ID, ISSUED_AT)).willReturn(false);

        Jwt result = decoder.decode(TOKEN);

        assertThat(result).isEqualTo(jwt);
    }

    @Test
    void decode_revokedToken_throwsBadJwtException() {

        given(delegate.decode(TOKEN)).willReturn(buildJwt());
        given(accessTokenRevocation.isRevoked(JTI, ACCOUNT_ID, ISSUED_AT)).willReturn(true);

        assertThatExceptionOfType(BadJwtException.class)
            .isThrownBy(() -> decoder.decode(TOKEN));
    }

    @Test
    void decode_revocationUnavailable_failsClosedAndPropagatesTheOutage() {

        var outage = new InfrastructureException("Redis unavailable — token revocation read failed for jti: " + JTI);
        given(delegate.decode(TOKEN)).willReturn(buildJwt());
        given(accessTokenRevocation.isRevoked(JTI, ACCOUNT_ID, ISSUED_AT)).willThrow(outage);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> decoder.decode(TOKEN))
            .isSameAs(outage);
    }

    // Without any one of them the token cannot be matched against either revocation reason, so it
    // is denied instead of consulted — a lookup on a missing claim would answer "not revoked".
    @ParameterizedTest
    @ValueSource(strings = {JwtClaimNames.JTI, JwtClaimNames.SUB, JwtClaimNames.IAT})
    void decode_tokenMissingAClaimTheCheckReads_isRejectedWithoutConsultingRevocation(String missingClaim) {

        given(delegate.decode(TOKEN)).willReturn(buildJwtWithout(missingClaim));

        assertThatExceptionOfType(BadJwtException.class)
            .isThrownBy(() -> decoder.decode(TOKEN));

        then(accessTokenRevocation).should(never()).isRevoked(any(), any(), any());
    }

    private static Jwt buildJwt() {

        return buildJwtWith(REVOCABLE_CLAIMS);
    }

    private static Jwt buildJwtWithout(String claim) {

        Map<String, Object> claims = new HashMap<>(REVOCABLE_CLAIMS);
        claims.remove(claim);

        return buildJwtWith(claims);
    }

    private static Jwt buildJwtWith(Map<String, Object> claims) {

        return Jwt.withTokenValue(TOKEN)
            .header("alg", "ES256")
            .expiresAt(Instant.now().plusSeconds(900))
            .claims(target -> target.putAll(claims))
            .build();
    }
}
