package com.valadir.persistence.adapter;

import com.valadir.application.port.out.UpdateProfilePersistence;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.User;
import com.valadir.persistence.config.PersistenceWiring;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
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
class UpdateProfilePersistenceJpaAdapterIT {

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UpdateProfilePersistence adapter;

    @AfterEach
    void cleanUp() {

        userJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
    }

    @Test
    void update_accountWithProfile_writesNamesAndLanguageAndLeavesOtherAccountsUntouched() {

        var account = AccountMother.active().build();
        var user = UserMother.builder().withAccountId(account.getId()).build();
        persist(account, user);

        var bystanderAccount = AccountMother.active().withEmail(Email.from("clark.kent@email.com")).build();
        var bystanderUser = UserMother.builder().withAccountId(bystanderAccount.getId()).build();
        persist(bystanderAccount, bystanderUser);

        var newFullName = FullName.from("Bruce Thomas Wayne");
        var newGivenName = GivenName.from("Matches Malone");

        adapter.update(account.changeLanguage(Language.ES), user.rename(newFullName, newGivenName));

        var storedAccount = accountJpaRepository.findById(account.getId().value()).orElseThrow();
        var storedUser = userJpaRepository.findByAccountId(account.getId().value()).orElseThrow();
        assertThat(storedAccount.getLanguage()).isEqualTo(Language.ES);
        assertThat(storedUser.getFullName()).isEqualTo(newFullName.value());
        assertThat(storedUser.getGivenName()).isEqualTo(newGivenName.value());

        var storedBystanderAccount = accountJpaRepository.findById(bystanderAccount.getId().value()).orElseThrow();
        var storedBystanderUser = userJpaRepository.findByAccountId(bystanderAccount.getId().value()).orElseThrow();
        assertThat(storedBystanderAccount.getLanguage()).isEqualTo(bystanderAccount.getLanguage());
        assertThat(storedBystanderUser.getFullName()).isEqualTo(bystanderUser.getFullName().value());
        assertThat(storedBystanderUser.getGivenName()).isEqualTo(bystanderUser.getGivenName().value());
    }

    // A PUT replaces the whole profile: a missing given name must clear the stored one, not keep it
    @Test
    void update_withoutGivenName_clearsTheStoredOne() {

        var account = AccountMother.active().build();
        var user = UserMother.builder().withAccountId(account.getId()).build();
        persist(account, user);

        adapter.update(account, user.rename(user.getFullName(), GivenName.from(null)));

        var storedUser = userJpaRepository.findByAccountId(account.getId().value()).orElseThrow();
        assertThat(storedUser.getGivenName()).isNull();
    }

    private void persist(Account account, User user) {

        accountJpaRepository.save(AccountMapper.toEntity(account));
        userJpaRepository.save(UserMapper.toEntity(user));
    }
}
