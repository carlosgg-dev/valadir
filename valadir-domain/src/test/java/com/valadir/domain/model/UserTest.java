package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void shouldCreateUser_WhenDataIsValid() {

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
    void shouldReconstituteUser() {

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
