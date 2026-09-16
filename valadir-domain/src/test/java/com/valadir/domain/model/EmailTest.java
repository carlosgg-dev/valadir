package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class EmailTest {

    @Test
    void constructor_validValue_createsEmail() {

        Email email = new Email("user@domain.com");
        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "bruce+wayne@email.com",
        "o'brien@email.com",
        "ñandú@correo.es",
        "ana@münchen.de",
        "ana@mail-1.co.uk"
    })
    void constructor_acceptedCharacters_createsEmail(String value) {

        assertThat(new Email(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "plainaddress",
        "@domain.com",
        "user@name@domain.com",
        "user@domain",
        "user@",
        "user@domain.",
        "user@.domain.com",
        "user@domain.com.",
        "user@.domain.com.",
        "user@domain..com",
        ".user@domain.com",
        "user.@domain.com",
        "us..er@domain.com",
        "user@domain.com@"
    })
    void constructor_invalidFormat_throwsDomainException(String invalidValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(invalidValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // Measured through MimeMessageHelper: a(b) and a<b> mail an address other than the stored one, the rest fail composition
    @ParameterizedTest
    @ValueSource(strings = {"a(b)@c.de", "a,b@c.de", "a<b>@c.de", "a[b]@c.de", "a:b@c.de", "a;b@c.de", "\"a\"@c.de"})
    void constructor_forbiddenLocalPartCharacter_throwsDomainException(String withForbiddenCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(withForbiddenCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@-b.de", "a@b-.de", "a@b_c.de", "a@b!c.de", "a@b©c.de", "a@[1.2.3.4]"})
    void constructor_invalidDomainLabel_throwsDomainException(String withInvalidLabel) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(withInvalidLabel))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void constructor_blankValue_throwsDomainException(String blankValue) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(blankValue))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\0", " \0 "})
    void constructor_blankOnceTrimmed_throwsDomainException(String blankOnceTrimmed) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(blankOnceTrimmed))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bruce wayne@email.com", "bruce\0wayne@email.com", "bruce.wayne@email\n.com"})
    void constructor_whitespaceOrControlCharacter_throwsDomainException(String withForbiddenCharacter) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(withForbiddenCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // No-break space, zero-width space, and a tag character outside the BMP that a char-by-char scan splits into surrogates
    @ParameterizedTest
    @ValueSource(ints = {0x00A0, 0x200B, 0xE0041})
    void constructor_invisibleCharacter_throwsDomainException(int codePoint) {

        var withInvisibleCharacter = "bruce" + Character.toString(codePoint) + "wayne@email.com";

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(withInvisibleCharacter))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // A lone high surrogate, a lone low one, and a pair in the wrong order. Only the local part let them through:
    // a surrogate is no letter, mark or digit, so the domain labels already refused it
    @ParameterizedTest
    @ValueSource(strings = {"bruce\uD800wayne@email.com", "bruce\uDC00wayne@email.com", "bruce\uDC00\uD800wayne@email.com"})
    void constructor_unpairedSurrogate_throwsDomainException(String withUnpairedSurrogate) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(withUnpairedSurrogate))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    // An emoji is a well-formed code point beyond the basic plane: visible, valid, and still no part of an address
    @Test
    void constructor_supplementaryCharacter_throwsDomainException() {

        var withEmoji = "bruce" + Character.toString(0x1F600) + "wayne@email.com";

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(withEmoji))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Bruce.Wayne@Email.com",
        "BRUCE.WAYNE@EMAIL.COM",
        "  bruce.wayne@email.com  ",
        " Bruce.Wayne@Email.com "
    })
    void constructor_caseOrSurroundingSpaces_normalizesToASingleValue(String value) {

        assertThat(new Email(value).value()).isEqualTo("bruce.wayne@email.com");
    }

    // U+00F1 is the ñ as one character, n + U+0303 is an n carrying a combining tilde: one address on screen, two to a
    // lookup by bytes. Written as escapes so a tool that normalized this file cannot turn the two fixtures into one,
    // which would leave the test passing against the very bug it was written for.
    @Test
    void constructor_theSameLetterSpelledTwoWays_normalizesToASingleValue() {

        var nfd = "pen\u0303a@espan\u0303a.com";

        assertThat(new Email(nfd).value()).isEqualTo("pe\u00F1a@espa\u00F1a.com");
    }

    @Test
    void constructor_localPartAtMaxLengthOnceComposed_createsEmail() {

        // 65 units as typed, 64 once composed: the length that counts is the one that reaches the column
        var localPart = "a".repeat(63);

        assertThat(new Email(localPart + "n\u0303@domain.com").value()).isEqualTo(localPart + "\u00F1@domain.com");
    }

    @Test
    void constructor_localPartAtMaxLength_createsEmail() {

        Email email = new Email("a".repeat(64) + "@domain.com");
        assertThat(email.value()).startsWith("a".repeat(64) + "@");
    }

    @Test
    void constructor_localPartTooLong_throwsDomainException() {

        var localPart = "a".repeat(65);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(localPart + "@domain.com"))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void constructor_domainLabelAtMaxLength_createsEmail() {

        var email = "a@" + "b".repeat(63) + ".com";

        assertThat(new Email(email).value()).isEqualTo(email);
    }

    @Test
    void constructor_domainLabelTooLong_throwsDomainException() {

        var email = "a@" + "b".repeat(64) + ".com";

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(email))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void constructor_encodedDomainAtMaxLength_createsEmail() {

        var email = "a@" + buildInternationalizedDomain(27);

        assertThat(new Email(email).value()).isEqualTo(email);
    }

    @Test
    void constructor_encodedDomainTooLong_throwsDomainException() {

        var email = "a@" + buildInternationalizedDomain(28);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(email))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void constructor_valueAtMaxLength_createsEmail() {

        var label = "b".repeat(63);
        Email email = new Email("a@" + String.join(".", label, label, label, "c".repeat(61)));
        assertThat(email.value()).hasSize(255);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var label = "b".repeat(63);
        var tooLong = "a@" + String.join(".", label, label, label, "c".repeat(62));

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void from_validValue_createsEmail() {

        Email email = Email.from("user@domain.com");
        assertThat(email).isEqualTo(new Email("user@domain.com"));
    }

    // Seven labels of thirteen ideographs encode to 31 ASCII characters each: the domain encodes to 228 plus the ASCII label
    private static String buildInternationalizedDomain(int asciiLabelLength) {

        var ideographs = IntStream.range(0, 13)
            .mapToObj(index -> Character.toString(0x4E00 + index * 37))
            .collect(Collectors.joining());

        return String.join(".", Collections.nCopies(7, ideographs)) + "." + "x".repeat(asciiLabelLength) + ".com";
    }
}
