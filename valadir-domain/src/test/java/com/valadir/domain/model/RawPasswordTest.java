package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

    @Test
    void shouldCreateRawPassword_WhenComplexityIsMet() {

        RawPassword password = new RawPassword("SecureP@ss123");
        assertThat(password.value()).isEqualTo("SecureP@ss123");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Short1!",
        "no_uppercase_1",
        "NO_LOWERCASE_1",
        "NoSpecialChar123",
        "NoDigit_Letters",
        "No_Numbers"
    })
    void shouldThrowException_WhenComplexityIsNotMet(String invalidPassword) {

        assertThatThrownBy(() -> new RawPassword(invalidPassword))
            .as("Password '%s' should be considered invalid", invalidPassword)
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @ParameterizedTest
    @MethodSource("provideNullAndBlankPasswords")
    void shouldThrowException_WhenValueIsNullOrBlank(String invalidPassword) {

        assertThatThrownBy(() -> new RawPassword(invalidPassword))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    private static String[] provideNullAndBlankPasswords() {

        return new String[]{null, "", " "};
    }
}
