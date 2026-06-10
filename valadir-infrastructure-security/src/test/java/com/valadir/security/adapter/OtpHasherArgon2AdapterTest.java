package com.valadir.security.adapter;

import com.valadir.domain.model.PlainOtp;
import com.valadir.test.mother.OtpMother;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OtpHasherArgon2AdapterTest {

    private static final Argon2PasswordEncoder ENCODER = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final OtpHasherArgon2Adapter hasher = new OtpHasherArgon2Adapter(ENCODER);

    private static final PlainOtp PLAIN_OTP = OtpMother.plain();

    @Test
    void hash_plainOtp_returnsArgon2Format() {

        assertThat(hasher.hash(PLAIN_OTP).value()).startsWith("$argon2id");
    }

    @Test
    void hash_sameOtp_producesDifferentHashes() {

        assertThat(hasher.hash(PLAIN_OTP).value()).isNotEqualTo(hasher.hash(PLAIN_OTP).value());
    }

    @Test
    void matches_correctOtp_returnsTrue() {

        assertThat(hasher.matches(PLAIN_OTP, hasher.hash(PLAIN_OTP))).isTrue();
    }

    @Test
    void matches_incorrectOtp_returnsFalse() {

        assertThat(hasher.matches(PlainOtp.generate(), hasher.hash(PLAIN_OTP))).isFalse();
    }

    @Test
    void guardTiming_doesNotThrow() {

        assertThatCode(hasher::guardTiming).doesNotThrowAnyException();
    }
}
