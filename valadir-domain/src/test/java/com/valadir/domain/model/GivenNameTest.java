package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GivenNameTest {

    @Test
    void new_valueProvided_createsGivenName() {

        GivenName givenName = new GivenName("Bruce");
        assertThat(givenName.value()).isEqualTo("Bruce");
    }

    @ParameterizedTest
    @MethodSource("provideBlankGivenNames")
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

        assertThatThrownBy(() -> new GivenName(tooLong))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    private static String[] provideBlankGivenNames() {

        return new String[]{null, "", "   "};
    }
}
