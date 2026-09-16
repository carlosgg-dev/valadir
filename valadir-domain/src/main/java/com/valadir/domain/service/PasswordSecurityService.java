package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.User;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PasswordSecurityService {

    private static final Pattern TERM_SEPARATOR = Pattern.compile("[\\s._-]+");
    private static final int MIN_TERM_LENGTH = 4;

    public void validatePassword(RawPassword password, Email email, User user) {

        String pwd = comparableFormOf(password.value());
        boolean containsEmail = pwd.contains(comparableFormOf(email.value()));

        Set<String> nameTerms = user.personalData().stream()
            .map(PasswordSecurityService::comparableFormOf)
            .flatMap(TERM_SEPARATOR::splitAsStream)
            .filter(term -> term.length() >= MIN_TERM_LENGTH)
            .collect(Collectors.toSet());

        if (containsEmail || nameTerms.stream().anyMatch(pwd::contains)) {
            throw new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);
        }
    }

    // One spelling and one case on both sides: the comparison produces the form it needs instead of trusting
    // whichever one each value object happened to keep
    private static String comparableFormOf(String value) {

        return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
