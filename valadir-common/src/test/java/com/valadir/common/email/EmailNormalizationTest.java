package com.valadir.common.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizationTest {

    private static final String CANONICAL = "bruce.wayne@email.com";

    // Case folding is locale dependent: in this one an uppercase I folds to a dotless "ı",
    // which resolves to a different account and a different rate-limit bucket.
    private static final Locale LOCALE_WITH_DIFFERENT_CASE_FOLDING = Locale.forLanguageTag("tr");

    @ParameterizedTest
    @ValueSource(strings = {
        "Bruce.Wayne@Email.com",
        "BRUCE.WAYNE@EMAIL.COM",
        "  bruce.wayne@email.com  ",
        " Bruce.Wayne@Email.com "
    })
    void canonicalOf_caseOrSurroundingSpaces_resolvesToASingleForm(String value) {

        assertThat(EmailNormalization.canonicalOf(value)).isEqualTo(CANONICAL);
    }

    // U+00F1 is the ñ as one character, n + U+0303 is an n carrying a combining tilde: one address on
    // screen, two to a lookup by bytes. Written as escapes so a tool that normalized this file cannot
    // turn the two fixtures into one, which would leave the test passing against the very bug it was
    // written for.
    @Test
    void canonicalOf_theSameLetterSpelledTwoWays_resolvesToASingleForm() {

        var nfd = "pen\u0303a@espan\u0303a.com";

        assertThat(EmailNormalization.canonicalOf(nfd)).isEqualTo("pe\u00F1a@espa\u00F1a.com");
    }

    // The uppercase I is on the input side only: with it on both, the two fold alike and the test
    // passes against the very bug it was written for.
    @Test
    void canonicalOf_foldsCaseIndependentlyOfTheDefaultLocale() {

        Locale defaultLocale = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_CASE_FOLDING);

            assertThat(EmailNormalization.canonicalOf("BRUCE.WAYNE@EMAIL.COM")).isEqualTo(CANONICAL);

        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
