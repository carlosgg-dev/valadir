package com.valadir.security.adapter;

import com.valadir.domain.model.RawPassword;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PasswordHasherArgon2AdapterTest {

    private static final Argon2PasswordEncoder ENCODER = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final PasswordHasherArgon2Adapter hasher = new PasswordHasherArgon2Adapter(ENCODER);

    private static final RawPassword RAW_PASSWORD = PasswordMother.raw();

    @Test
    void hash_rawPassword_returnsArgon2Format() {

        assertThat(hasher.hash(RAW_PASSWORD).value()).startsWith("$argon2id");
    }

    @Test
    void hash_samePassword_producesDifferentHashes() {

        assertThat(hasher.hash(RAW_PASSWORD).value()).isNotEqualTo(hasher.hash(RAW_PASSWORD).value());
    }

    @Test
    void matches_correctPassword_returnsTrue() {

        assertThat(hasher.matches(RAW_PASSWORD, hasher.hash(RAW_PASSWORD))).isTrue();
    }

    @Test
    void matches_incorrectPassword_returnsFalse() {

        var otherPassword = RawPassword.from("Different#Pass99");

        assertThat(hasher.matches(otherPassword, hasher.hash(RAW_PASSWORD))).isFalse();
    }

    @Test
    void guardTiming_anyPassword_doesNotThrow() {

        assertThatCode(() -> hasher.guardTiming(RAW_PASSWORD)).doesNotThrowAnyException();
    }
}
