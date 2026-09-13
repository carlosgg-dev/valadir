package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Language {

    EN(Locale.ENGLISH),
    ES(Locale.forLanguageTag("es"));

    private static final Language DEFAULT = EN;

    private final Locale locale;

    Language(Locale locale) {

        this.locale = locale;
    }

    // Strict, unlike forTag: a language the owner picked explicitly and we do not write is a rejected
    // request, never a silent switch to English.
    public static Language from(String languageTag) {

        if (languageTag == null || languageTag.isBlank()) {
            throw new DomainException("Language is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }

        return supportedLanguageOf(languageTag)
            .orElseThrow(() -> new DomainException("Unsupported language", ErrorCode.INVALID_FIELD));
    }

    public static Language forTag(String languageTag) {

        if (languageTag == null || languageTag.isBlank()) {
            return DEFAULT;
        }

        return supportedLanguageOf(languageTag).orElse(DEFAULT);
    }

    public String tag() {

        return locale.toLanguageTag();
    }

    public Locale toLocale() {

        return locale;
    }

    // Compares the language subtag alone, so a tag carrying a region ("es-ES") still matches.
    private static Optional<Language> supportedLanguageOf(String languageTag) {

        var language = Locale.forLanguageTag(languageTag).getLanguage();

        return Arrays.stream(values())
            .filter(candidate -> candidate.locale.getLanguage().equals(language))
            .findFirst();
    }
}
