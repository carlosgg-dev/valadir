package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    void from_validData_createsAccount() {

        AccountId id = AccountId.generate();
        Email email = new Email("bruce.wayne@email.com");
        HashedPassword hashedPassword = new HashedPassword("$2a$12$hashedpassword");
        Role role = Role.USER;

        Account account = Account.from(id, email, hashedPassword, role);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getPassword()).isEqualTo(hashedPassword);
        assertThat(account.getRole()).isEqualTo(role);
    }

    @Test
    void reconstitute_validData_reconstitutesAccount() {

        AccountId id = AccountId.generate();
        Email email = new Email("bruce.wayne@email.com");
        HashedPassword hashedPassword = new HashedPassword("$2a$12$hashedpassword");
        Role role = Role.ADMIN;

        Account account = Account.reconstitute(id, email, hashedPassword, role);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getPassword()).isEqualTo(hashedPassword);
        assertThat(account.getRole()).isEqualTo(role);
    }
}
