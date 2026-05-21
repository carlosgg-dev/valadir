package com.valadir.application.otp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HashedOtpTest {

    @Test
    void constructor_validValue_storesValue() {

        var hashedOtp = new HashedOtp("$argon2id$v=19$hashedValue");

        assertThat(hashedOtp.value()).isEqualTo("$argon2id$v=19$hashedValue");
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValues_throwsIllegalArgumentException(String blankValue) {

        assertThatIllegalArgumentException().isThrownBy(() -> new HashedOtp(blankValue));
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
