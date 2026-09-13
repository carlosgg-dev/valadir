package com.valadir.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private static final AccountId ID = AccountId.generate();
    private static final Email EMAIL = Email.from("bruce.wayne@email.com");
    private static final HashedPassword PASSWORD = new HashedPassword("$2a$12$hashedpassword");
    private static final Role ROLE = Role.USER;
    // Deliberately not the fallback language: an accidental EN default would go unnoticed.
    private static final Language LANGUAGE = Language.ES;

    @Test
    void newPendingActivation_validData_createsAccountPendingActivation() {

        var account = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE, LANGUAGE);

        assertThat(account.isActive()).isFalse();
        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(account.getPassword()).isEqualTo(PASSWORD);
        assertThat(account.getRole()).isEqualTo(ROLE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(account.getLanguage()).isEqualTo(LANGUAGE);
    }

    @Test
    void reconstitute_validData_reconstitutesAccount() {

        var account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE, LANGUAGE);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getEmail()).isEqualTo(EMAIL);
        assertThat(account.getPassword()).isEqualTo(PASSWORD);
        assertThat(account.getRole()).isEqualTo(ROLE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getLanguage()).isEqualTo(LANGUAGE);
    }

    @Test
    void activate_pendingActivationAccount_returnsNewInstanceWithActiveStatus() {

        var pending = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE, LANGUAGE);
        Account activated = pending.activate();

        assertThat(activated.isActive()).isTrue();
        assertThat(pending.isActive()).isFalse();
    }

    @Test
    void isActive_activeAccount_returnsTrue() {

        var account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE, LANGUAGE);

        assertThat(account.isActive()).isTrue();
    }

    @Test
    void isActive_pendingActivationAccount_returnsFalse() {

        var account = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE, LANGUAGE);

        assertThat(account.isActive()).isFalse();
    }

    @Test
    void isPendingActivation_activeAccount_returnsFalse() {

        var account = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE, LANGUAGE);

        assertThat(account.isPendingActivation()).isFalse();
    }

    @Test
    void isPendingActivation_pendingActivationAccount_returnsTrue() {

        var account = Account.newPendingActivation(ID, EMAIL, PASSWORD, ROLE, LANGUAGE);

        assertThat(account.isPendingActivation()).isTrue();
    }

    @Test
    void changeLanguage_otherLanguage_returnsNewInstanceKeepingEverythingElse() {

        var original = Account.reconstitute(ID, EMAIL, PASSWORD, ROLE, AccountStatus.ACTIVE, LANGUAGE);
        var otherLanguage = Language.EN;

        Account changed = original.changeLanguage(otherLanguage);

        assertThat(changed.getLanguage()).isEqualTo(otherLanguage);
        assertThat(changed.getId()).isEqualTo(ID);
        assertThat(changed.getEmail()).isEqualTo(EMAIL);
        assertThat(changed.getPassword()).isEqualTo(PASSWORD);
        assertThat(changed.getRole()).isEqualTo(ROLE);
        assertThat(changed.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(original.getLanguage()).isEqualTo(LANGUAGE);
    }
}
