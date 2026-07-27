package com.valadir.persistence.adapter;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.GivenName;
import com.valadir.persistence.config.PersistenceWiring;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import com.valadir.test.containers.PostgresContainerConfig;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.UserMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

// Runs without a test-managed transaction so the adapter executes with the same
// transactional semantics as production — a missing @Transactional fails here.
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({PostgresContainerConfig.class, PersistenceWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegisterPersistenceJpaAdapterIT {

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RegisterPersistence adapter;

    @AfterEach
    void cleanUp() {

        userJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
    }

    @Test
    void save_validAccountAndUser_persistsBoth() {

        var accountId = AccountId.generate();
        var account = AccountMother.pendingActivation().withId(accountId).build();
        var user = UserMother.builder().withAccountId(accountId).build();

        adapter.save(account, user);

        assertThat(accountJpaRepository.findById(accountId.value())).isPresent();
        assertThat(userJpaRepository.findById(user.getId().value())).isPresent();
    }

    // given_name is the one nullable column of the profile: registering without it must reach the
    // database, and only a real one tells us whether the column still allows it.
    @Test
    void save_userWithoutGivenName_persistsIt() {

        var accountId = AccountId.generate();
        var account = AccountMother.pendingActivation().withId(accountId).build();
        var user = UserMother.builder()
            .withAccountId(accountId)
            .withGivenName(GivenName.from(null))
            .build();

        adapter.save(account, user);

        var persisted = userJpaRepository.findById(user.getId().value()).orElseThrow();

        assertThat(persisted.getGivenName()).isNull();
        assertThat(persisted.getFullName()).isEqualTo(user.getFullName().value());
    }

    // Two concurrent registrations of the same email both find it free and both insert; the unique
    // index rejects the second one. Untranslated it surfaces as an opaque 500 instead of a 409.
    @Test
    void save_emailTakenByAnotherAccount_throwsEmailAlreadyExists() {

        var emailOwnerAccountId = AccountId.generate();
        var emailOwnerAccount = AccountMother.pendingActivation().withId(emailOwnerAccountId).build();
        var emailOwnerUser = UserMother.builder().withAccountId(emailOwnerAccountId).build();

        adapter.save(emailOwnerAccount, emailOwnerUser);

        var collidingAccountId = AccountId.generate();
        var collidingAccount = AccountMother.pendingActivation().withId(collidingAccountId).build();
        var collidingUser = UserMother.builder().withAccountId(collidingAccountId).build();

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> adapter.save(collidingAccount, collidingUser))
            .withCauseInstanceOf(DataIntegrityViolationException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        assertThat(accountJpaRepository.findById(collidingAccountId.value())).isEmpty();
    }

    @Test
    void replace_deletesExistingAndPersistsNew() {

        var existingAccountId = AccountId.generate();
        var existingAccount = AccountMother.pendingActivation().withId(existingAccountId).build();
        var existingUser = UserMother.builder().withAccountId(existingAccountId).build();

        adapter.save(existingAccount, existingUser);

        var newAccountId = AccountId.generate();
        var newAccount = AccountMother.pendingActivation().withId(newAccountId).build();
        var newUser = UserMother.builder().withAccountId(newAccountId).build();

        adapter.replace(existingAccountId, newAccount, newUser);

        assertThat(accountJpaRepository.findById(existingAccountId.value())).isEmpty();
        assertThat(userJpaRepository.findById(existingUser.getId().value())).isEmpty();

        assertThat(accountJpaRepository.findById(newAccountId.value())).isPresent();
        assertThat(userJpaRepository.findById(newUser.getId().value())).isPresent();
    }

    // Two concurrent re-registrations resolve the same pending account and both replace it. Every
    // account here carries the same email, so the second INSERT lands on the one the first took.
    @Test
    void replace_accountAlreadyReplacedByAConcurrentRegistration_throwsEmailAlreadyExists() {

        // P (id = P, email = X) — the account both registrations resolved
        var pendingAccountId = AccountId.generate();
        var pendingAccount = AccountMother.pendingActivation().withId(pendingAccountId).build();
        var pendingUser = UserMother.builder().withAccountId(pendingAccountId).build();

        adapter.save(pendingAccount, pendingUser);

        // A (id = A, email = X) — replaces P.id: P is deleted, X is now A's
        var replacingAccountId = AccountId.generate();
        var replacingAccount = AccountMother.pendingActivation().withId(replacingAccountId).build();
        var replacingUser = UserMother.builder().withAccountId(replacingAccountId).build();

        adapter.replace(pendingAccountId, replacingAccount, replacingUser);

        // B (id = B, email = X) — replaces P.id too: P is already gone and X belongs to A
        var collidingAccountId = AccountId.generate();
        var collidingAccount = AccountMother.pendingActivation().withId(collidingAccountId).build();
        var collidingUser = UserMother.builder().withAccountId(collidingAccountId).build();

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> adapter.replace(pendingAccountId, collidingAccount, collidingUser))
            .withCauseInstanceOf(DataIntegrityViolationException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        // The rejected registration leaves no trace: the account that won the race is the only one.
        assertThat(accountJpaRepository.findById(replacingAccountId.value())).isPresent();
        assertThat(accountJpaRepository.count()).isEqualTo(1);
    }
}
