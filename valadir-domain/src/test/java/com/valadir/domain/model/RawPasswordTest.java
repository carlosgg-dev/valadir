package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RawPasswordTest {

    @Test
    void constructor_valueFailingPolicy_createsRawPassword() {

        RawPassword password = new RawPassword("weak");
        assertThat(password.value()).isEqualTo("weak");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new RawPassword(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void constructor_valueAtMaxLength_createsRawPassword() {

        RawPassword rawPassword = new RawPassword("a".repeat(72));
        assertThat(rawPassword.value()).hasSize(72);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(73);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new RawPassword(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void from_valueFailingPolicy_createsRawPassword() {

        RawPassword password = RawPassword.from("weak");
        assertThat(password).isEqualTo(new RawPassword("weak"));
    }

    @Test
    void newPassword_policyMet_createsRawPassword() {

        RawPassword password = RawPassword.newPassword("SecureP@ss123");
        assertThat(password).isEqualTo(new RawPassword("SecureP@ss123"));
    }

    @Test
    void newPassword_valueAtMinLength_createsRawPassword() {

        RawPassword password = RawPassword.newPassword("Aa1!aaaa");
        assertThat(password.value()).hasSize(8);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Short1!",
        "no_uppercase_1",
        "NO_LOWERCASE_1",
        "NoSpecialChar123",
        "NoDigit_Letters"
    })
    void newPassword_policyNotMet_throwsDomainException(String invalidPassword) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> RawPassword.newPassword(invalidPassword))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }
}
