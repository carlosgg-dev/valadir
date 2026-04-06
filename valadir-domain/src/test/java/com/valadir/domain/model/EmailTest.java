package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        assertThatThrownBy(() -> new Email(invalidEmail))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @MethodSource("provideBlankEmails")
    void new_blankValue_throwsDomainException(String invalidEmail) {

        assertThatThrownBy(() -> new Email(invalidEmail))
            .isInstanceOf(DomainException.class)
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

        assertThatThrownBy(() -> new Email(localPart + "@domain.com"))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    private static String[] provideBlankEmails() {

        return new String[]{null, "", "   "};
    }
}
