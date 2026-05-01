package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GivenNameTest {

    @Test
    void new_valueProvided_createsGivenName() {

        GivenName givenName = new GivenName("Bruce");
        assertThat(givenName.value()).isEqualTo("Bruce");
    }

    @ParameterizedTest
    @MethodSource("blankGivenNames")
    void new_blankValue_storesNull(String blankGivenName) {

        GivenName givenName = new GivenName(blankGivenName);
        assertThat(givenName.value()).isNull();
    }

    @Test
    void empty_createsGivenNameWithNullValue() {

        GivenName givenName = GivenName.empty();
        assertThat(givenName.value()).isNull();
    }

    @Test
    void new_valueAtMaxLength_createsGivenName() {

        GivenName givenName = new GivenName("a".repeat(100));
        assertThat(givenName.value()).hasSize(100);
    }

    @Test
    void new_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(101);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    private static String[] blankGivenNames() {

        return new String[]{null, "", "   "};
    }
}
