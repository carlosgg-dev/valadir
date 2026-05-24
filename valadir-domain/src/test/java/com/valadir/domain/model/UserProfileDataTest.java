package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UserProfileDataTest {

    @Test
    void constructor_bothValuesProvided_createsUserProfileData() {

        FullName fullName = FullName.from("Bruce Wayne");
        GivenName givenName = GivenName.from("Batman");
        UserProfileData profileData = new UserProfileData(fullName, givenName);

        assertThat(profileData.fullName()).isEqualTo(fullName);
        assertThat(profileData.givenName()).isEqualTo(givenName);
    }

    @Test
    void constructor_nullGivenName_normalizesToEmpty() {

        FullName fullName = FullName.from("Bruce Wayne");
        UserProfileData profileData = new UserProfileData(fullName, null);

        assertThat(profileData.givenName()).isEqualTo(GivenName.empty());
    }

    @Test
    void constructor_nullFullName_throwsDomainException() {

        GivenName givenName = GivenName.from("Batman");

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> new UserProfileData(null, givenName))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void from_bothValuesProvided_createsUserProfileData() {

        FullName fullName = FullName.from("Bruce Wayne");
        GivenName givenName = GivenName.from("Batman");
        UserProfileData profileData = UserProfileData.from(fullName, givenName);

        assertThat(profileData).isEqualTo(new UserProfileData(fullName, givenName));
    }

    @Test
    void values_fullNameAndGivenName_returnsBothValues() {

        FullName fullName = FullName.from("Bruce Wayne");
        GivenName givenName = GivenName.from("Batman");
        var profileData = new UserProfileData(fullName, givenName);

        assertThat(profileData.values()).isEqualTo(Set.of("Bruce Wayne", "Batman"));
    }

    @Test
    void values_noGivenName_returnsOnlyFullName() {

        FullName fullName = FullName.from("Bruce Wayne");
        var profileData = new UserProfileData(fullName, null);

        assertThat(profileData.values()).isEqualTo(Set.of("Bruce Wayne"));
    }
}
