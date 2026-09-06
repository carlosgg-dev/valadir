package com.valadir.notifications.mail;

import com.valadir.domain.model.Language;
import com.valadir.notifications.config.MailRenderingTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class DurationWordingTest {

    private static final Duration LOCKOUT = Duration.ofMinutes(30);
    private static final Duration SINGLE_MINUTE = Duration.ofMinutes(1);
    private static final Duration SUB_MINUTE = Duration.ofSeconds(45);
    private static final Duration SINGLE_SECOND = Duration.ofSeconds(1);

    // Its numbering system renders digits as Arabic-Indic, so a duration formatted with the default
    // locale reaches the reader as a number they cannot compare with the one they were told.
    private static final Locale LOCALE_WITH_DIFFERENT_DIGITS = Locale.forLanguageTag("ar-EG-u-nu-arab");

    private final DurationWording durationWording = MailRenderingTestFactory.durationWording();

    @ParameterizedTest
    @CsvSource({"EN, 30 minutes", "ES, 30 minutos"})
    void wordingOf_severalMinutes_readsInThatLanguage(Language language, String expected) {

        assertThat(durationWording.wordingOf(LOCKOUT, language.toLocale())).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"EN, 1 minute", "ES, 1 minuto"})
    void wordingOf_oneMinute_readsInSingular(Language language, String expected) {

        assertThat(durationWording.wordingOf(SINGLE_MINUTE, language.toLocale())).isEqualTo(expected);
    }

    // Truncating to minutes would announce a "0 minutes" lockout as soon as a tier drops below one.
    @ParameterizedTest
    @CsvSource({"EN, 45 seconds", "ES, 45 segundos"})
    void wordingOf_subMinuteDuration_readsInSeconds(Language language, String expected) {

        assertThat(durationWording.wordingOf(SUB_MINUTE, language.toLocale())).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"EN, 1 second", "ES, 1 segundo"})
    void wordingOf_oneSecond_readsInSingular(Language language, String expected) {

        assertThat(durationWording.wordingOf(SINGLE_SECOND, language.toLocale())).isEqualTo(expected);
    }

    @Test
    void wordingOf_exoticDefaultLocale_keepsWesternDigits() {

        var originalDefault = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_DIGITS);

            assertThat(durationWording.wordingOf(LOCKOUT, Language.EN.toLocale())).isEqualTo("30 minutes");
        } finally {
            Locale.setDefault(originalDefault);
        }
    }
}
