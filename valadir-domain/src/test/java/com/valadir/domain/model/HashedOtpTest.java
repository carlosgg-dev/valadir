package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HashedOtpTest {

    @Test
    void constructor_validValue_storesValue() {

        var hashedOtp = new HashedOtp("$argon2id$v=19$hashedValue");

        assertThat(hashedOtp.value()).isEqualTo("$argon2id$v=19$hashedValue");
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValues_throwsIllegalArgumentException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new HashedOtp(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
