package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.User;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PasswordSecurityService {

    private static final Pattern TERM_SEPARATOR = Pattern.compile("[\\s._-]+");
    private static final int MIN_TERM_LENGTH = 4;

    public void validatePassword(RawPassword password, Email email, User user) {

        String pwd = password.value().toLowerCase(Locale.ROOT);
        boolean containsEmail = pwd.contains(email.value());

        Set<String> nameTerms = user.personalData().stream()
            .map(term -> term.toLowerCase(Locale.ROOT))
            .flatMap(TERM_SEPARATOR::splitAsStream)
            .filter(term -> term.length() >= MIN_TERM_LENGTH)
            .collect(Collectors.toSet());

        if (containsEmail || nameTerms.stream().anyMatch(pwd::contains)) {
            throw new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);
        }
    }
}
