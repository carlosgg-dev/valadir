package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FullNameTest {

    @Test
    void constructor_validValue_createsFullName() {

        FullName fullName = new FullName("Bruce Wayne");
        assertThat(fullName.value()).isEqualTo("Bruce Wayne");
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void constructor_valueAtMinLength_createsFullName() {

        FullName fullName = new FullName("Wa");
        assertThat(fullName.value()).isEqualTo("Wa");
    }

    @Test
    void constructor_valueTooShort_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName("W"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void constructor_valueAtMaxLength_createsFullName() {

        FullName fullName = new FullName("a".repeat(255));
        assertThat(fullName.value()).hasSize(255);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(256);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void from_validValue_createsFullName() {

        FullName fullName = FullName.from("Bruce Wayne");
        assertThat(fullName).isEqualTo(new FullName("Bruce Wayne"));
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
