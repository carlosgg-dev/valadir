package com.valadir.security.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenFingerprintTest {

    // The SHA-256 of "token", pinned by hand: it is what turns a swapped algorithm red.
    private static final String TOKEN = "token";
    private static final String TOKEN_SHA_256 = "3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153e0";

    @Test
    void constructor_nullValue_throws() {

        assertThatThrownBy(() -> new TokenFingerprint(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // A raw token is exactly what must never reach the constructor: it is the mistake the type exists
    // to make impossible, and a UUID fails on both its length and its hyphens.
    @ParameterizedTest
    @ValueSource(strings = {
        "b7f9c2a1-3e4d-4f6a-8b1c-2d3e4f5a6b7c",
        "3C469E9D6C5875D37A43F353D4F88E61FCF812C66EEE3457465A40B0DA4153E0",
        "3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153e",
        "3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153e0f",
        "3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153eg",
        ""
    })
    void constructor_valueThatIsNotASha256Digest_throws(String value) {

        assertThatThrownBy(() -> new TokenFingerprint(value))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_knownToken_returnsItsSha256InLowercaseHex() {

        assertThat(TokenFingerprint.of(TOKEN).value()).isEqualTo(TOKEN_SHA_256);
    }

    // The whole design rests on this: the lookup happens by the token, so the same token must
    // always address the same key.
    @Test
    void of_sameTokenTwice_returnsTheSameFingerprint() {

        var token = UUID.randomUUID().toString();

        assertThat(TokenFingerprint.of(token)).isEqualTo(TokenFingerprint.of(token));
    }

    @Test
    void of_differentTokens_returnDifferentFingerprints() {

        assertThat(TokenFingerprint.of(UUID.randomUUID().toString()))
            .isNotEqualTo(TokenFingerprint.of(UUID.randomUUID().toString()));
    }

    @Test
    void of_token_doesNotCarryTheTokenItself() {

        var token = UUID.randomUUID().toString();

        assertThat(TokenFingerprint.of(token).value()).doesNotContain(token);
    }
}
