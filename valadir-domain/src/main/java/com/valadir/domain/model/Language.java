package com.valadir.domain.model;

import java.util.Arrays;
import java.util.Locale;

public enum Language {

    EN(Locale.ENGLISH),
    ES(Locale.forLanguageTag("es"));

    private static final Language DEFAULT = EN;

    private final Locale locale;

    Language(Locale locale) {

        this.locale = locale;
    }

    public static Language forTag(String languageTag) {

        if (languageTag == null || languageTag.isBlank()) {
            return DEFAULT;
        }

        // Compares the language subtag alone, so a tag carrying a region ("es-ES") still matches.
        var language = Locale.forLanguageTag(languageTag).getLanguage();

        return Arrays.stream(values())
            .filter(candidate -> candidate.locale.getLanguage().equals(language))
            .findFirst()
            .orElse(DEFAULT);
    }

    public Locale toLocale() {

        return locale;
    }
}
