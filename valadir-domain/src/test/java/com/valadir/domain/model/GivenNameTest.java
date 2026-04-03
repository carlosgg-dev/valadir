package com.valadir.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static String[] provideBlankGivenNames() {

        return new String[]{null, "", "   "};
    }
}
