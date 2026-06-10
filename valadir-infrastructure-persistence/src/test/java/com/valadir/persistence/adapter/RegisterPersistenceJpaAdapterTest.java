package com.valadir.persistence.adapter;

import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.domain.model.AccountId;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// Runs without a test-managed transaction so the adapter executes with the same
// transactional semantics as production — a missing @Transactional fails here.
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({PostgresContainerConfig.class, PersistenceWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegisterPersistenceJpaAdapterTest {

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
}
