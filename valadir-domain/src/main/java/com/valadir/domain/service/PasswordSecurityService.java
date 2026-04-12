package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.UserProfileData;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class PasswordSecurityService {

    private static final int MIN_TERM_LENGTH = 4;

    public void validatePassword(RawPassword password, Email email, UserProfileData profileData) {

        final String pwd = password.value().toLowerCase();

        final boolean containsEmail = pwd.contains(email.value().toLowerCase());

        final Set<String> nameTerms = profileData.values().stream()
            .map(String::toLowerCase)
            .flatMap(term -> Arrays.stream(term.split("[\\s._-]+")))
            .filter(term -> term.length() >= MIN_TERM_LENGTH)
            .collect(Collectors.toSet());

        if (containsEmail || nameTerms.stream().anyMatch(pwd::contains)) {
            throw new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);
        }
    }
}
