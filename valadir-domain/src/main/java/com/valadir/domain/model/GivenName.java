package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record GivenName(String value) {

    private static final String LABEL = "Given name";

    private static final int MAX_LENGTH = 100;

    public GivenName {

        value = normalize(value);

        if (value != null) {
            Names.rejectHiddenCharacters(value, LABEL);
            requireMaxLength(value);
        }
    }

    public static GivenName from(String value) {

        return new GivenName(value);
    }

    // A given name is optional, so what is blank once normalized is absent rather than invalid
    private static String normalize(String value) {

        String normalized = Names.normalize(value);

        return normalized == null || normalized.isBlank()
            ? null
            : normalized;
    }

    private static void requireMaxLength(String value) {

        if (value.length() > MAX_LENGTH) {
            throw new DomainException(LABEL + " must not exceed " + MAX_LENGTH + " characters", ErrorCode.INVALID_FIELD);
        }
    }
}
