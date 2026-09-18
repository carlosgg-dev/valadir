package com.valadir.domain.model;

import com.valadir.common.email.EmailNormalization;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.net.IDN;
import java.util.regex.Pattern;

public record Email(String value) {

    // The accounts.email column: the domain never produces a value the schema cannot store
    private static final int MAX_LENGTH = 255;
    // RFC 5321
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    // RFC 1035, and punycode can make an IDN domain several times longer than what was typed
    private static final int MAX_ENCODED_DOMAIN_LENGTH = 255;
    private static final int MIN_DOMAIN_LABELS = 2;

    // RFC 5322 atext, the atoms of an unquoted local part: anything else fails mail composition or changes the address the mail leaves for
    private static final Pattern LOCAL_PART_ATOM = Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~\\P{ASCII}-]+");

    // A hostname, not an address: symbols like _ and ! name no host
    private static final Pattern LABEL = Pattern.compile("[\\p{L}\\p{M}\\p{N}-]+");

    public Email {

        value = normalize(value);
        requirePresent(value);
        rejectWhitespace(value);
        rejectNonPrintingCharacters(value);
        rejectUnpairedSurrogates(value);
        rejectSupplementaryCharacters(value);
        requireMaxLength(value);
        requireValidStructure(value);
    }

    public static Email from(String value) {

        return new Email(value);
    }

    // Exactly one @ is the constructor's guarantee, so the split needs no check of its own
    public String localPart() {

        return value.substring(0, value.indexOf('@'));
    }

    private static String normalize(String value) {

        return value == null
            ? null
            : EmailNormalization.canonicalOf(value);
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

    // A code point scan pairs the surrogates first, so only an unpaired one is left reading as SURROGATE; the driver would
    // encode it as '?' and store an address nobody typed
    private static void rejectUnpairedSurrogates(String value) {

        if (value.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.SURROGATE)) {
            throw new DomainException("Email must not contain unpaired surrogates", ErrorCode.INVALID_FIELD);
        }
    }

    // Nothing beyond the basic plane names a host or a mailbox, and stating it here is what keeps the format patterns plain
    private static void rejectSupplementaryCharacters(String value) {

        if (value.codePoints().anyMatch(codePoint -> !Character.isBmpCodePoint(codePoint))) {
            throw new DomainException("Email must not contain supplementary characters", ErrorCode.INVALID_FIELD);
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
            throw invalidFormat();
        }

        requireValidLocalPart(parts[0]);
        requireValidDomain(parts[1]);
    }

    private static void requireValidLocalPart(String localPart) {

        if (localPart.length() > MAX_LOCAL_PART_LENGTH) {
            throw new DomainException("Email local part must not exceed " + MAX_LOCAL_PART_LENGTH + " characters", ErrorCode.INVALID_FIELD);
        }

        // A leading, trailing or doubled dot leaves an empty atom, which no atom of one character or more matches
        String[] atoms = localPart.split("\\.", -1);
        for (String atom : atoms) {
            if (!LOCAL_PART_ATOM.matcher(atom).matches()) {
                throw invalidFormat();
            }
        }
    }

    private static void requireValidDomain(String domain) {

        String[] labels = domain.split("\\.", -1);

        // A single label is a host on the local network, never a public mailbox
        if (labels.length < MIN_DOMAIN_LABELS) {
            throw invalidFormat();
        }

        for (String label : labels) {
            requireValidLabel(label);
        }

        if (!fitsDnsLimits(domain)) {
            throw invalidFormat();
        }
    }

    private static void requireValidLabel(String label) {

        if (label.startsWith("-") || label.endsWith("-") || !LABEL.matcher(label).matches()) {
            throw invalidFormat();
        }
    }

    // IDN.toASCII refuses a label over 63 characters once encoded, and the encoded name has its own limit of 255
    private static boolean fitsDnsLimits(String domain) {

        try {
            return IDN.toASCII(domain).length() <= MAX_ENCODED_DOMAIN_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static DomainException invalidFormat() {

        return new DomainException("Invalid email format", ErrorCode.INVALID_FIELD);
    }
}
