package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.UserProfileData;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PasswordSecurityService {

    private static final int MIN_TERM_LENGTH = 4;

    public void validatePassword(RawPassword password, Email email, UserProfileData profileData) {

        String pwd = password.value().toLowerCase();
        String fullEmail = email.value();
        String fullNameValue = profileData.fullName().value();
        GivenName givenName = profileData.givenName();
        String givenNameValue = Optional.ofNullable(givenName).map(GivenName::value).orElse("");

        boolean containsEmail = pwd.contains(fullEmail.toLowerCase());

        Set<String> nameTerms = Stream.of(fullNameValue, givenNameValue)
            .filter(term -> !term.isBlank())
            .map(String::toLowerCase)
            .flatMap(term -> Stream.of(term.split("[\\s._-]+")))
            .filter(term -> term.length() >= MIN_TERM_LENGTH)
            .collect(Collectors.toSet());

        if (containsEmail || nameTerms.stream().anyMatch(pwd::contains)) {
            throw new DomainException("Password cannot contain your personal data", ErrorCode.INSECURE_PASSWORD);
        }
    }
}
