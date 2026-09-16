package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.text.Normalizer;
import java.util.function.IntPredicate;

public record RawPassword(String value) {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    // Shape only: a presented password is matched against its hash, so one chosen under an older policy still signs in
    public RawPassword {

        if (value == null || value.isBlank()) {
            throw new DomainException("Password cannot be empty", ErrorCode.INVALID_PASSWORD);
        }

        // The hash is over bytes, so the two spellings of an accented letter are two secrets and the keyboard decides
        // which one arrives. Composing here is what makes it the same password on the next device
        value = Normalizer.normalize(value, Normalizer.Form.NFC);

        if (value.length() > MAX_PASSWORD_LENGTH) {
            throw new DomainException("Password is too long", ErrorCode.INVALID_PASSWORD);
        }
    }

    public static RawPassword from(String value) {

        return new RawPassword(value);
    }

    public static RawPassword newPassword(String value) {

        var password = new RawPassword(value);

        if (!password.meetsPolicy()) {
            throw new DomainException(
                "The password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.",
                ErrorCode.INVALID_PASSWORD
            );
        }

        return password;
    }

    private boolean meetsPolicy() {

        return value.length() >= MIN_PASSWORD_LENGTH
            && contains(Character::isDigit)
            && contains(Character::isLowerCase)
            && contains(Character::isUpperCase)
            && contains(codePoint -> !Character.isLetterOrDigit(codePoint));
    }

    // Code points, not chars: a letter beyond the basic plane reaches a char scan as two surrogates that are neither
    private boolean contains(IntPredicate characterClass) {

        return value.codePoints().anyMatch(characterClass);
    }
}
