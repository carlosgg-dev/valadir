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
    void constructor_validValue_createsEmail() {

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
    void constructor_invalidFormat_throwsDomainException(String invalidValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(invalidValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void constructor_valueAtMaxLength_createsEmail() {

        String localPart = "a".repeat(244);
        Email email = new Email(localPart + "@domain.com");
        assertThat(email.value()).hasSize(255);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        String localPart = "a".repeat(245);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(localPart + "@domain.com"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void from_validValue_createsEmail() {

        Email email = Email.from("user@domain.com");
        assertThat(email).isEqualTo(new Email("user@domain.com"));
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
