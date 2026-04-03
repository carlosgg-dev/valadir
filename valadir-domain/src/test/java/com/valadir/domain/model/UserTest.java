package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createNewProfile_validData_createsUser() {

        UserId id = UserId.generate();
        AccountId accountId = AccountId.generate();
        FullName fullName = new FullName("Bruce Wayne");
        GivenName givenName = new GivenName("Batman");

        User user = User.createNewProfile(id, accountId, fullName, givenName);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getAccountId()).isEqualTo(accountId);
        assertThat(user.getFullName()).isEqualTo(fullName);
        assertThat(user.getGivenName()).isEqualTo(givenName);
    }

    @Test
    void reconstitute_validData_reconstitutesUser() {

        UserId id = UserId.generate();
        AccountId accountId = AccountId.generate();
        FullName fullName = new FullName("Bruce Wayne");
        GivenName givenName = new GivenName("Batman");

        User user = User.reconstitute(id, accountId, fullName, givenName);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getAccountId()).isEqualTo(accountId);
        assertThat(user.getFullName()).isEqualTo(fullName);
        assertThat(user.getGivenName()).isEqualTo(givenName);
    }
}
