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
    void constructor_validValue_createsGivenName() {

        GivenName givenName = new GivenName("Batman");
        assertThat(givenName.value()).isEqualTo("Batman");
    }

    @ParameterizedTest
    @MethodSource("blankValues")
    void constructor_blankValue_storesNull(String blankValue) {

        GivenName givenName = new GivenName(blankValue);
        assertThat(givenName.value()).isNull();
    }

    @Test
    void constructor_valueAtMaxLength_createsGivenName() {

        GivenName givenName = new GivenName("a".repeat(100));
        assertThat(givenName.value()).hasSize(100);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(101);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void from_validValue_createsGivenName() {

        GivenName givenName = GivenName.from("Batman");
        assertThat(givenName).isEqualTo(new GivenName("Batman"));
    }

    private static String[] blankValues() {

        return new String[]{null, "", "   "};
    }
}
