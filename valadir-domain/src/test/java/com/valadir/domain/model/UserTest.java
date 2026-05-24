package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void newProfile_validData_createsUser() {

        UserId id = UserId.generate();
        AccountId accountId = AccountId.generate();
        FullName fullName = FullName.from("Bruce Wayne");
        GivenName givenName = GivenName.from("Batman");

        User user = User.newProfile(id, accountId, fullName, givenName);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getAccountId()).isEqualTo(accountId);
        assertThat(user.getFullName()).isEqualTo(fullName);
        assertThat(user.getGivenName()).isEqualTo(givenName);
    }

    @Test
    void reconstitute_validData_reconstitutesUser() {

        UserId id = UserId.generate();
        AccountId accountId = AccountId.generate();
        FullName fullName = FullName.from("Bruce Wayne");
        GivenName givenName = GivenName.from("Batman");

        User user = User.reconstitute(id, accountId, fullName, givenName);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getAccountId()).isEqualTo(accountId);
        assertThat(user.getFullName()).isEqualTo(fullName);
        assertThat(user.getGivenName()).isEqualTo(givenName);
    }
}
