package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FullNameTest {

    @Test
    void constructor_validValue_createsFullName() {

        FullName fullName = new FullName("Bruce Wayne");
        assertThat(fullName.value()).isEqualTo("Bruce Wayne");
    }

    @Test
    void constructor_surroundingSpaces_trimsValue() {

        FullName fullName = new FullName("  Bruce Wayne  ");
        assertThat(fullName.value()).isEqualTo("Bruce Wayne");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\0", " \0 ", "\u00A0"})
    void constructor_blankOnceTrimmed_throwsDomainException(String blankOnceTrimmed) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(blankOnceTrimmed))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void constructor_valueAtMinLength_createsFullName() {

        FullName fullName = new FullName("Wa");
        assertThat(fullName.value()).isEqualTo("Wa");
    }

    @Test
    void constructor_valueTooShortOnceTrimmed_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(" W"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void constructor_valueTooShort_throwsDomainException() {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName("W"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // U+00F1 is the ñ as one character, n + U+0303 is an n carrying a combining tilde: one name on screen, two rows no
    // reader can tell apart. Written as escapes so a tool that normalized this file cannot turn the two fixtures into
    // one, which would leave the test passing against the very bug it was written for.
    @Test
    void constructor_theSameLetterSpelledTwoWays_composesToASingleValue() {

        var nfd = "Pen\u0303a Wayne";

        assertThat(new FullName(nfd).value()).isEqualTo("Pe\u00F1a Wayne");
    }

    @Test
    void constructor_valueAtMaxLengthOnceComposed_createsFullName() {

        // 256 units as typed, 255 once composed: the length that counts is the one that reaches the column
        FullName fullName = new FullName("a".repeat(254) + "n\u0303");
        assertThat(fullName.value()).hasSize(255);
    }

    @Test
    void constructor_valueAtMaxLength_createsFullName() {

        FullName fullName = new FullName("a".repeat(255));
        assertThat(fullName.value()).hasSize(255);
    }

    @Test
    void constructor_valueAtMaxLengthOnceTrimmed_createsFullName() {

        FullName fullName = new FullName(" " + "a".repeat(255) + " ");
        assertThat(fullName.value()).hasSize(255);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var tooLong = "a".repeat(256);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bruce\0Wayne", "Bruce\nWayne", "Bruce\tWayne"})
    void constructor_controlCharacter_throwsDomainException(String withControlCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(withControlCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // No-break space, thin space and ideographic space: what a paste leaves behind, and all of them read as one space
    @ParameterizedTest
    @ValueSource(strings = {"Bruce\u00A0Wayne", "Bruce\u2009Wayne", "Bruce\u3000Wayne"})
    void constructor_exoticSpace_foldsItToAPlainSpace(String withExoticSpace) {

        assertThat(new FullName(withExoticSpace).value()).isEqualTo("Bruce Wayne");
    }

    @Test
    void constructor_exoticSpaceAtTheEdges_trimsIt() {

        assertThat(new FullName("\u00A0Bruce Wayne\u00A0").value()).isEqualTo("Bruce Wayne");
    }

    // Zero-width space, zero-width joiner, right-to-left mark and soft hyphen: each renders as nothing, so each makes
    // a second name that no reader can tell from the first
    @ParameterizedTest
    @ValueSource(strings = {"Bruce\u200BWayne", "Bruce\u200DWayne", "Bruce\u200FWayne", "Bruce\u00ADWayne"})
    void constructor_invisibleFormattingCharacter_throwsDomainException(String withInvisibleCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(withInvisibleCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // A lone high surrogate, a lone low one, and a pair in the wrong order
    @ParameterizedTest
    @ValueSource(strings = {"Bruce\uD800Wayne", "Bruce\uDC00Wayne", "Bruce\uDC00\uD800Wayne"})
    void constructor_unpairedSurrogate_throwsDomainException(String withUnpairedSurrogate) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new FullName(withUnpairedSurrogate))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // What is rejected is half a character, not everything outside the BMP: a well-formed emoji is a name people do use
    @Test
    void constructor_surrogatePair_createsFullName() {

        var withEmoji = "Bruce " + Character.toString(0x1F600) + " Wayne";

        assertThat(new FullName(withEmoji).value()).isEqualTo(withEmoji);
    }

    @Test
    void from_validValue_createsFullName() {

        FullName fullName = FullName.from("Bruce Wayne");
        assertThat(fullName).isEqualTo(new FullName("Bruce Wayne"));
    }

}
