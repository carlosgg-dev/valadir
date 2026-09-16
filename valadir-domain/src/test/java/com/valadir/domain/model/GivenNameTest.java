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

    @Test
    void constructor_surroundingSpaces_trimsValue() {

        GivenName givenName = new GivenName("  Batman  ");
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
    @ValueSource(strings = {"\0", " \0 ", "\u00A0"})
    void constructor_blankOnceTrimmed_storesNull(String blankOnceTrimmed) {

        GivenName givenName = new GivenName(blankOnceTrimmed);
        assertThat(givenName.value()).isNull();
    }

    // The same letter as one character and as an n carrying a combining tilde, written as escapes so a tool that
    // normalized this file cannot turn the two fixtures into one
    @Test
    void constructor_theSameLetterSpelledTwoWays_composesToASingleValue() {

        var nfd = "Pen\u0303a";

        assertThat(new GivenName(nfd).value()).isEqualTo("Pe\u00F1a");
    }

    @Test
    void constructor_valueAtMaxLength_createsGivenName() {

        GivenName givenName = new GivenName("a".repeat(100));
        assertThat(givenName.value()).hasSize(100);
    }

    @Test
    void constructor_valueAtMaxLengthOnceTrimmed_createsGivenName() {

        GivenName givenName = new GivenName(" " + "a".repeat(100) + " ");
        assertThat(givenName.value()).hasSize(100);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(101);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bat\0man", "Bat\nman", "Bat\tman"})
    void constructor_controlCharacter_throwsDomainException(String withControlCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(withControlCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // No-break space, thin space and ideographic space: what a paste leaves behind, and all of them read as one space
    @ParameterizedTest
    @ValueSource(strings = {"Bat\u00A0man", "Bat\u2009man", "Bat\u3000man"})
    void constructor_exoticSpace_foldsItToAPlainSpace(String withExoticSpace) {

        assertThat(new GivenName(withExoticSpace).value()).isEqualTo("Bat man");
    }

    // Zero-width space, zero-width joiner, right-to-left mark and soft hyphen: each renders as nothing, so each makes
    // a second name that no reader can tell from the first
    @ParameterizedTest
    @ValueSource(strings = {"Bat\u200Bman", "Bat\u200Dman", "Bat\u200Fman", "Bat\u00ADman"})
    void constructor_invisibleFormattingCharacter_throwsDomainException(String withInvisibleCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new GivenName(withInvisibleCharacter))
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
