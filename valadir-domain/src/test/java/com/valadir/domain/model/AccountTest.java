package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private static final AccountId ID = AccountId.generate();
    private static final Email EMAIL = Email.from("bruce.wayne@email.com");
    private static final HashedPassword PASSWORD = new HashedPassword("$2a$12$hashedpassword");
    private static final Role ROLE = Role.USER;

    @Test
    void newPendingActivation_validData_createsAccountPendingActivation() {

        var account = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(account.getPassword()).isEqualTo(PASSWORD);
        assertThat(account.getRole()).isEqualTo(ROLE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(account.isActive()).isFalse();
    }

    @Test
    void reconstitute_validData_reconstitutesAccount() {

        var account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(account.getPassword()).isEqualTo(PASSWORD);
        assertThat(account.getRole()).isEqualTo(ROLE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void activate_pendingActivationAccount_returnsNewInstanceWithActiveStatus() {

        var pending = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE);
        Account activated = pending.activate();

        assertThat(activated.isActive()).isTrue();
        assertThat(pending.isActive()).isFalse();
    }

    @Test
    void isActive_activeAccount_returnsTrue() {

        var account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE);

        assertThat(account.isActive()).isTrue();
    }

    @Test
    void isActive_pendingActivationAccount_returnsFalse() {

        var account = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE);

        assertThat(account.isActive()).isFalse();
    }

    @Test
    void isPendingActivation_activeAccount_returnsFalse() {

        var account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE);

        assertThat(account.isPendingActivation()).isFalse();
    }

    @Test
    void isPendingActivation_pendingActivationAccount_returnsTrue() {

        var account = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE);

        assertThat(account.isPendingActivation()).isTrue();
    }
}
