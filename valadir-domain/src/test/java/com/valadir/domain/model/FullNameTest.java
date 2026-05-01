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
    void new_validValue_createsFullName() {

        FullName fullName = new FullName("Bruce Wayne");
        assertThat(fullName.value()).isEqualTo("Bruce Wayne");
    }

    @ParameterizedTest
    @MethodSource("provideBlankFullNames")
    void new_blankValue_throwsDomainException(String blankFullName) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(blankFullName))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void new_valueAtMinLength_createsFullName() {

        FullName fullName = new FullName("Wa");
        assertThat(fullName.value()).isEqualTo("Wa");
    }

    @Test
    void new_valueTooShort_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName("W"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void new_valueAtMaxLength_createsFullName() {

        FullName fullName = new FullName("a".repeat(255));
        assertThat(fullName.value()).hasSize(255);
    }

    @Test
    void new_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(256);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    private static String[] provideBlankFullNames() {

        return new String[]{null, "", "   "};
    }
}
