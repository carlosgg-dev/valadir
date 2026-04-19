package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private static final AccountId ID = AccountId.generate();
    private static final Email EMAIL = new Email("bruce.wayne@email.com");
    private static final HashedPassword PASSWORD = new HashedPassword("$2a$12$hashedpassword");
    private static final Role ROLE = Role.USER;

    @Test
    void newPendingVerification_validData_createsAccountWithPendingVerification() {

        Account account = Account.newPendingVerification(ID, EMAIL, PASSWORD, ROLE);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(account.getPassword()).isEqualTo(PASSWORD);
        assertThat(account.getRole()).isEqualTo(ROLE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(account.isActive()).isFalse();
    }

    @Test
    void reconstitute_validData_reconstitutesAccount() {

        Account account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(account.getPassword()).isEqualTo(PASSWORD);
        assertThat(account.getRole()).isEqualTo(ROLE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void activate_pendingAccount_returnsNewInstanceWithActiveStatus() {

        Account pending = Account.newPendingVerification(ID, EMAIL, PASSWORD, ROLE);

        Account activated = pending.activate();

        assertThat(activated.isActive()).isTrue();
        assertThat(activated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(pending.isActive()).isFalse();
    }

    @Test
    void isActive_activeAccount_returnsTrue() {

        Account account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE);

        assertThat(account.isActive()).isTrue();
    }

    @Test
    void isActive_pendingAccount_returnsFalse() {

        Account account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.PENDING_VERIFICATION);

        assertThat(account.isActive()).isFalse();
    }
}
