package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LanguageTest {

    @Test
    void from_supportedTag_resolvesThatLanguage() {

        assertThat(Language.from("es")).isEqualTo(Language.ES);
    }

    @Test
    void from_supportedTagCarryingARegion_resolvesTheLanguageAlone() {

        assertThat(Language.from("es-MX")).isEqualTo(Language.ES);
    }

    // Chosen, not negotiated: answering English here would store a language nobody picked.
    @ParameterizedTest
    @ValueSource(strings = {"fr", "tlh", "12345", "not a language tag"})
    void from_unsupportedOrMalformedTag_throwsDomainException(String languageTag) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> Language.from(languageTag))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FIELD);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void from_blankTag_throwsDomainException(String blankTag) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> Language.from(blankTag))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void forTag_supportedTag_resolvesThatLanguage() {

        assertThat(Language.forTag("es")).isEqualTo(Language.ES);
    }

    @Test
    void forTag_supportedTagCarryingARegion_resolvesTheLanguageAlone() {

        assertThat(Language.forTag("es-MX")).isEqualTo(Language.ES);
    }

    // A reader asking for a language we do not write still gets their mail, in English.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"tlh", "", "   ", "12345", "not a language tag"})
    void forTag_unsupportedOrMalformedTag_fallsBackToEnglish(String languageTag) {

        assertThat(Language.forTag(languageTag)).isEqualTo(Language.EN);
    }

    @Test
    void forTag_exoticDefaultLocale_stillResolvesTheRequestedTag() {

        // Turkish folds an uppercase I to a dotless i, so a case-insensitive match made with the
        // default locale would stop recognising "ES" as Spanish wherever the JVM runs in tr-TR.
        var originalDefault = Locale.getDefault();

        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(Language.forTag("ES")).isEqualTo(Language.ES);
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    @ParameterizedTest
    @CsvSource({"EN, en", "ES, es"})
    void tag_anyLanguage_returnsItsLanguageTag(Language language, String expectedLanguageTag) {

        assertThat(language.tag()).isEqualTo(expectedLanguageTag);
    }

    @ParameterizedTest
    @CsvSource({"EN, en", "ES, es"})
    void toLocale_anyLanguage_returnsTheLocaleItWrites(Language language, String expectedLanguageTag) {

        assertThat(language.toLocale().getLanguage()).isEqualTo(expectedLanguageTag);
    }
}
