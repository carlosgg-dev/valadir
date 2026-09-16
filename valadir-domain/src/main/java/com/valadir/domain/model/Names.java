package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * The text rules a person's name obeys, in one place so two names cannot drift apart.
 *
 * <p>The three rejections always apply together and in this order, so they are one call rather than three: a name type
 * that took two of the three would be the very gap these rules were written to close.
 */
final class Names {

    private static final Pattern SPACE_SEPARATOR = Pattern.compile("\\p{Z}");

    private Names() {

    }

    // Every separator folds to a plain space before the trim: a name pasted from a word processor is not a different
    // name. Composed last, so the two spellings of an accented letter are not two profiles nobody can tell apart
    static String normalize(String value) {

        return value == null
            ? null
            : Normalizer.normalize(SPACE_SEPARATOR.matcher(value).replaceAll(" ").trim(), Normalizer.Form.NFC);
    }

    /**
     * Rejects every character that does not show what it is.
     *
     * <p>The label names the field in the message. A 4xx is logged without its stack trace, so the message is the only
     * place left that can say which name was refused.
     */
    static void rejectHiddenCharacters(String value, String label) {

        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new DomainException(label + " must not contain control characters", ErrorCode.INVALID_FIELD);
        }

        // What renders as nothing must not be stored as something: two names that read alike are two names nobody can tell apart
        if (value.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.FORMAT)) {
            throw new DomainException(label + " must not contain invisible formatting characters", ErrorCode.INVALID_FIELD);
        }

        // A code point scan pairs the surrogates first, so only an unpaired one is left reading as SURROGATE; the driver
        // would encode it as '?' and store a name nobody typed
        if (value.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.SURROGATE)) {
            throw new DomainException(label + " must not contain unpaired surrogates", ErrorCode.INVALID_FIELD);
        }
    }
}
