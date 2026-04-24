package com.valadir.security.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2OtpHasherTest {

    private static final Argon2PasswordEncoder ENCODER = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final Argon2OtpHasher hasher = new Argon2OtpHasher(ENCODER);

    private static final String PLAIN_CODE = "123456";

    @Test
    void hash_plainCode_returnsArgon2Format() {

        assertThat(hasher.hash(PLAIN_CODE)).startsWith("$argon2id");
    }

    @Test
    void hash_sameCode_producesDifferentHashes() {

        assertThat(hasher.hash(PLAIN_CODE)).isNotEqualTo(hasher.hash(PLAIN_CODE));
    }

    @Test
    void matches_correctCode_returnsTrue() {

        assertThat(hasher.matches(PLAIN_CODE, hasher.hash(PLAIN_CODE))).isTrue();
    }

    @Test
    void matches_incorrectCode_returnsFalse() {

        assertThat(hasher.matches("654321", hasher.hash(PLAIN_CODE))).isFalse();
    }
}
