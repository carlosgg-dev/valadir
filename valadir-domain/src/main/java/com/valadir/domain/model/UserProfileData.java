package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record UserProfileData(FullName fullName, GivenName givenName) {

    public UserProfileData {

        if (fullName == null) {
            throw new DomainException("Full name is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }

        if (givenName == null) {
            givenName = GivenName.empty();
        }
    }

    public Set<String> values() {

        return Stream.of(fullName().value(), givenName().value())
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toSet());
    }
}
