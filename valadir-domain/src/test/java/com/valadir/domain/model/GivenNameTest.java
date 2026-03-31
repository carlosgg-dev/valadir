package com.valadir.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class GivenNameTest {

    @Test
    void shouldCreateGivenName_WhenValueIsProvided() {

        GivenName givenName = new GivenName("Bruce");
        assertThat(givenName.value()).isEqualTo("Bruce");
    }

    @ParameterizedTest
    @MethodSource("provideBlankGivenNames")
    void shouldCreateGivenName_WithNull_WhenValueIsBlank(String blankGivenName) {

        GivenName givenName = new GivenName(blankGivenName);
        assertThat(givenName.value()).isNull();
    }

    @Test
    void shouldCreateGivenName_WithNull_WhenUsingEmptyFactory() {

        GivenName givenName = GivenName.empty();
        assertThat(givenName.value()).isNull();
    }

    private static String[] provideBlankGivenNames() {

        return new String[]{null, "", "   "};
    }
}
