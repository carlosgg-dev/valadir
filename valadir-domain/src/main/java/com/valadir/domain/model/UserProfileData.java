package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;

public record UserProfileData(FullName fullName, GivenName givenName) {

    public UserProfileData {

        if (fullName == null) {
            throw new DomainException("Full name is required", ErrorCode.REQUIRED_FIELD_MISSING);
        }

        if (givenName == null) {
            givenName = GivenName.empty();
        }
    }
}
