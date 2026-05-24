package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HashedPasswordTest {

    @Test
    void constructor_validValue_createsHashedPassword() {

        var password = "$2a$10$someHashValue";
        var hashed = new HashedPassword(password);

        assertThat(hashed.value()).isEqualTo(password);
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new HashedPassword(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
