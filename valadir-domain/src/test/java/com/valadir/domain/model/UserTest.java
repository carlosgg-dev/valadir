package com.valadir.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void newProfile_validData_createsUser() {

        var id = UserId.generate();
        var accountId = AccountId.generate();
        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");

        var user = User.newProfile(id, accountId, fullName, givenName);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getAccountId()).isEqualTo(accountId);
        assertThat(user.getFullName()).isEqualTo(fullName);
        assertThat(user.getGivenName()).isEqualTo(givenName);
    }

    @Test
    void reconstitute_validData_reconstitutesUser() {

        var id = UserId.generate();
        var accountId = AccountId.generate();
        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");

        var user = User.reconstitute(id, accountId, fullName, givenName);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getAccountId()).isEqualTo(accountId);
        assertThat(user.getFullName()).isEqualTo(fullName);
        assertThat(user.getGivenName()).isEqualTo(givenName);
    }

    @Test
    void personalData_withGivenName_returnsBothValues() {

        var fullName = FullName.from("Bruce Wayne");
        var givenName = GivenName.from("Batman");

        var user = User.newProfile(
            UserId.generate(),
            AccountId.generate(),
            FullName.from(fullName.value()),
            GivenName.from(givenName.value())
        );

        assertThat(user.personalData()).containsExactly(fullName.value(), givenName.value());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void personalData_withBlankGivenName_returnsOnlyFullName(String blankGivenName) {

        var fullName = FullName.from("Bruce Wayne");

        var user = User.newProfile(
            UserId.generate(),
            AccountId.generate(),
            FullName.from(fullName.value()),
            GivenName.from(blankGivenName)
        );

        assertThat(user.personalData()).containsExactly(fullName.value());
    }

    @Test
    void rename_newNames_returnsNewInstanceKeepingItsIdentity() {

        var originalFullName = FullName.from("Bruce Wayne");
        var original = User.reconstitute(UserId.generate(), AccountId.generate(), originalFullName, GivenName.from("Batman"));

        var fullName = FullName.from("Bruce Thomas Wayne");
        var givenName = GivenName.from("Matches Malone");

        var renamed = original.rename(fullName, givenName);

        assertThat(renamed.getId()).isEqualTo(original.getId());
        assertThat(renamed.getAccountId()).isEqualTo(original.getAccountId());
        assertThat(renamed.getFullName()).isEqualTo(fullName);
        assertThat(renamed.getGivenName()).isEqualTo(givenName);
        assertThat(original.getFullName()).isEqualTo(originalFullName);
    }
}
