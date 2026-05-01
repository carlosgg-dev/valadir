package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class EmailTest {

    @Test
    void new_validEmail_createsEmail() {

        Email email = new Email("user@domain.com");
        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "plainaddress",
        "@domain.com",
        "user@name@domain.com",
        "user@domain",
        "user@"
    })
    void new_invalidFormat_throwsDomainException(String invalidEmail) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(invalidEmail))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @MethodSource("blankEmails")
    void new_blankValue_throwsDomainException(String blankEmail) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(blankEmail))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void new_valueAtMaxLength_createsEmail() {

        String localPart = "a".repeat(244);
        Email email = new Email(localPart + "@domain.com");
        assertThat(email.value()).hasSize(255);
    }

    @Test
    void new_valueTooLong_throwsDomainException() {

        String localPart = "a".repeat(245);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(localPart + "@domain.com"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    private static String[] blankEmails() {

        return new String[]{null, "", "   "};
    }
}
