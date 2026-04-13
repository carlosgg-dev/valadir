package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileDataTest {

    @Test
    void new_bothValuesProvided_createsUserProfileData() {

        FullName fullName = new FullName("Bruce Wayne");
        GivenName givenName = new GivenName("Bruce");

        UserProfileData profileData = new UserProfileData(fullName, givenName);

        assertThat(profileData.fullName()).isEqualTo(fullName);
        assertThat(profileData.givenName()).isEqualTo(givenName);
    }

    @Test
    void new_nullGivenName_normalizesToEmpty() {

        FullName fullName = new FullName("Bruce Wayne");

        UserProfileData profileData = new UserProfileData(fullName, null);

        assertThat(profileData.givenName()).isEqualTo(GivenName.empty());
    }

    @Test
    void new_nullFullName_throwsDomainException() {

        GivenName givenName = new GivenName("Bruce");

        assertThatThrownBy(() -> new UserProfileData(null, givenName))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUIRED_FIELD_MISSING);
    }

    @Test
    void values_fullNameAndGivenName_returnsBothValues() {

        var profileData = new UserProfileData(new FullName("Bruce Wayne"), new GivenName("Batman"));

        assertThat(profileData.values()).isEqualTo(Set.of("Bruce Wayne", "Batman"));
    }

    @Test
    void values_noGivenName_returnsOnlyFullName() {

        var profileData = new UserProfileData(new FullName("Bruce Wayne"), null);

        assertThat(profileData.values()).isEqualTo(Set.of("Bruce Wayne"));
    }
}
