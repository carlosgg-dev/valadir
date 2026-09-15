package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.util.Arrays;
import java.util.Locale;

public record Email(String value) {

    private static final int MAX_LENGTH = 255;
    private static final int MAX_LOCAL_PART_LENGTH = 64;

    public Email {

        value = normalize(value);
        requirePresent(value);
        rejectWhitespace(value);
        rejectNonPrintingCharacters(value);
        requireMaxLength(value);
        requireValidStructure(value);
    }

    public static Email from(String value) {

        return new Email(value);
    }

    private static String normalize(String value) {

        return value == null
            ? null
            : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void requirePresent(String value) {

        if (value == null || value.isBlank()) {
            throw new DomainException("Email is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }

    // isWhitespace misses the no-break spaces, isSpaceChar misses tabs and line breaks
    private static void rejectWhitespace(String value) {

        if (value.chars().anyMatch(character -> Character.isWhitespace(character) || Character.isSpaceChar(character))) {
            throw new DomainException("Email must not contain whitespace", ErrorCode.INVALID_FIELD);
        }
    }

    private static void rejectNonPrintingCharacters(String value) {

        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new DomainException("Email must not contain control characters", ErrorCode.INVALID_FIELD);
        }

        // Code points, not chars: some formatting characters lie beyond the BMP and a char scan only sees their surrogates
        if (value.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.FORMAT)) {
            throw new DomainException("Email must not contain invisible formatting characters", ErrorCode.INVALID_FIELD);
        }
    }

    private static void requireMaxLength(String value) {

        if (value.length() > MAX_LENGTH) {
            throw new DomainException("Email must not exceed " + MAX_LENGTH + " characters", ErrorCode.INVALID_FIELD);
        }
    }

    private static void requireValidStructure(String value) {

        // -1 keeps a trailing empty part, so a trailing @ still counts as a second delimiter
        String[] parts = value.split("@", -1);

        if (parts.length != 2) {
            throw new DomainException("Invalid email format", ErrorCode.INVALID_FIELD);
        }

        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() > MAX_LOCAL_PART_LENGTH) {
            throw new DomainException("Email local part must not exceed " + MAX_LOCAL_PART_LENGTH + " characters", ErrorCode.INVALID_FIELD);
        }

        if (hasEmptySegment(localPart) || hasEmptySegment(domain) || !domain.contains(".")) {
            throw new DomainException("Invalid email format", ErrorCode.INVALID_FIELD);
        }
    }

    // A leading, trailing or doubled dot leaves an empty segment; -1 keeps the trailing one, which split drops by default
    private static boolean hasEmptySegment(String part) {

        return Arrays.asList(part.split("\\.", -1)).contains("");
    }
}
