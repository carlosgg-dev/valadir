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
import java.util.stream.Stream;

public class PasswordSecurityService {

    // `+` among them so a plus-addressed local part does not hide behind its tag as one long term
    private static final Pattern TERM_SEPARATOR = Pattern.compile("[\\s._+-]+");
    private static final int MIN_TERM_LENGTH = 4;

    public void validatePassword(RawPassword password, Email email, User user) {

        String pwd = comparableFormOf(password.value());
        boolean containsEmail = pwd.contains(comparableFormOf(email.value()));

        Set<String> personalTerms = Stream.concat(user.personalData().stream(), Stream.of(email.localPart()))
            .map(PasswordSecurityService::comparableFormOf)
            .flatMap(TERM_SEPARATOR::splitAsStream)
            .filter(term -> term.length() >= MIN_TERM_LENGTH)
            .collect(Collectors.toSet());

        // The whole address stays its own check: a local part below the threshold contributes no term
        if (containsEmail || personalTerms.stream().anyMatch(pwd::contains)) {
            throw new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);
        }
    }

    // One spelling and one case on both sides: the comparison produces the form it needs instead of trusting
    // whichever one each value object happened to keep
    private static String comparableFormOf(String value) {

        return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
