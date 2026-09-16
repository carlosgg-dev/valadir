package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GivenNameTest {

    @Test
    void constructor_validValue_createsGivenName() {

        GivenName givenName = new GivenName("Batman");
        assertThat(givenName.value()).isEqualTo("Batman");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_blankValue_storesNull(String blankValue) {

        GivenName givenName = new GivenName(blankValue);
        assertThat(givenName.value()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\0", " \0 "})
    void constructor_blankOnceTrimmed_storesNull(String blankOnceTrimmed) {

        GivenName givenName = new GivenName(blankOnceTrimmed);
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
    void constructor_surroundingSpaces_trimsValue() {

        GivenName givenName = new GivenName("  Batman  ");
        assertThat(givenName.value()).isEqualTo("Batman");
    }

    @Test
    void constructor_valueAtMaxLengthOnceTrimmed_createsGivenName() {

        GivenName givenName = new GivenName(" " + "a".repeat(100) + " ");
        assertThat(givenName.value()).hasSize(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bat\0man", "Bat\nman", "Bat\tman"})
    void constructor_controlCharacter_throwsDomainException(String withControlCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(withControlCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // A lone high surrogate, a lone low one, and a pair in the wrong order
    @ParameterizedTest
    @ValueSource(strings = {"Bat\uD800man", "Bat\uDC00man", "Bat\uDC00\uD800man"})
    void constructor_unpairedSurrogate_throwsDomainException(String withUnpairedSurrogate) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(withUnpairedSurrogate))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // What is rejected is half a character, not everything outside the BMP: a well-formed emoji is a name people do use
    @Test
    void constructor_surrogatePair_createsGivenName() {

        var withEmoji = "Bat" + Character.toString(0x1F600) + "man";

        assertThat(new GivenName(withEmoji).value()).isEqualTo(withEmoji);
    }

    @Test
    void from_validValue_createsGivenName() {

        GivenName givenName = GivenName.from("Batman");
        assertThat(givenName).isEqualTo(new GivenName("Batman"));
    }

}
