package com.valadir.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageTest {

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
    void toLocale_anyLanguage_returnsTheLocaleItWrites(Language language, String expectedLanguageTag) {

        assertThat(language.toLocale().getLanguage()).isEqualTo(expectedLanguageTag);
    }
}
