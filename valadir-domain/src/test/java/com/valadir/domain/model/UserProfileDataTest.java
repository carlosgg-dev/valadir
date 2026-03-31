package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileDataTest {

    @Test
    void shouldCreateUserProfileData_WhenBothValuesAreProvided() {

        FullName fullName = new FullName("Bruce Wayne");
        GivenName givenName = new GivenName("Bruce");

        UserProfileData profileData = new UserProfileData(fullName, givenName);

        assertThat(profileData.fullName()).isEqualTo(fullName);
        assertThat(profileData.givenName()).isEqualTo(givenName);
    }

    @Test
    void shouldNormalizeGivenName_WhenGivenNameIsNull() {

        FullName fullName = new FullName("Bruce Wayne");

        UserProfileData profileData = new UserProfileData(fullName, null);

        assertThat(profileData.givenName()).isEqualTo(GivenName.empty());
    }

    @Test
    void shouldThrowException_WhenFullNameIsNull() {

        GivenName givenName = new GivenName("Bruce");

        assertThatThrownBy(() -> new UserProfileData(null, givenName))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }
}
