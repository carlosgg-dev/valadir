package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    void constructor_valueAtMaxLength_createsEmail() {

        Email email = new Email("a@" + "b".repeat(249) + ".com");
        assertThat(email.value()).hasSize(255);
    }

    @Test
    void constructor_valueTooLong_throwsDomainException() {

        var tooLong = "a@" + "b".repeat(250) + ".com";

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new Email(tooLong))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @Test
    void from_validValue_createsEmail() {

        Email email = Email.from("user@domain.com");
        assertThat(email).isEqualTo(new Email("user@domain.com"));
    }
}
