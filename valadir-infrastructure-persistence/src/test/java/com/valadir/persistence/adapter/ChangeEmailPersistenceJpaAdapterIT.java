package com.valadir.persistence.adapter;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.ChangeEmailPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.Email;
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
class ChangeEmailPersistenceJpaAdapterIT {

    private static final Email NEW_EMAIL = Email.from("matches.malone@email.com");

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ChangeEmailPersistence adapter;

    @AfterEach
    void cleanUp() {

        userJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
    }

    @Test
    void change_freeEmail_writesItAndLeavesOtherAccountsUntouched() {

        var account = AccountMother.active().build();
        var user = UserMother.builder().withAccountId(account.getId()).build();
        persist(account, user);

        var bystanderAccount = AccountMother.active().withEmail(Email.from("clark.kent@email.com")).build();
        var bystanderUser = UserMother.builder().withAccountId(bystanderAccount.getId()).build();
        persist(bystanderAccount, bystanderUser);

        adapter.change(account.getId(), NEW_EMAIL);

        assertThat(accountJpaRepository.findById(account.getId().value()).orElseThrow().getEmail())
            .isEqualTo(NEW_EMAIL.value());
        assertThat(accountJpaRepository.findById(bystanderAccount.getId().value()).orElseThrow().getEmail())
            .isEqualTo(bystanderAccount.getEmail().value());
    }

    @Test
    void changeReplacing_emailHeldByAPendingAccount_removesThatAccountAndItsProfileAndTakesTheEmail() {

        var account = AccountMother.active().build();
        var user = UserMother.builder().withAccountId(account.getId()).build();
        persist(account, user);

        var pendingAccount = AccountMother.pendingActivation().withEmail(NEW_EMAIL).build();
        var pendingUser = UserMother.builder().withAccountId(pendingAccount.getId()).build();
        persist(pendingAccount, pendingUser);

        adapter.changeReplacing(pendingAccount.getId(), account.getId(), NEW_EMAIL);

        assertThat(accountJpaRepository.findById(pendingAccount.getId().value())).isEmpty();
        assertThat(userJpaRepository.findById(pendingUser.getId().value())).isEmpty();
        assertThat(accountJpaRepository.findByEmail(NEW_EMAIL.value()).orElseThrow().getId()).isEqualTo(account.getId().value());
    }

    // The holder check runs before the write: an account activated on the address in between hits the unique index,
    // and untranslated that surfaces as an opaque 500 instead of a 409.
    @Test
    void change_emailTakenByAnotherAccount_throwsApplicationExceptionAndKeepsTheEmail() {

        var account = AccountMother.active().build();
        var id = account.getId();
        var user = UserMother.builder().withAccountId(id).build();
        persist(account, user);

        var holderAccount = AccountMother.active().withEmail(NEW_EMAIL).build();
        var holderUser = UserMother.builder().withAccountId(holderAccount.getId()).build();
        persist(holderAccount, holderUser);

        assertThatExceptionOfType(ApplicationException.class)
            .isThrownBy(() -> adapter.change(id, NEW_EMAIL))
            .withCauseInstanceOf(DataIntegrityViolationException.class)
            .extracting(ApplicationException::getErrorCode)
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        assertThat(accountJpaRepository.findById(id.value()).orElseThrow().getEmail())
            .isEqualTo(account.getEmail().value());
    }

    private void persist(Account account, User user) {

        accountJpaRepository.save(AccountMapper.toEntity(account));
        userJpaRepository.save(UserMapper.toEntity(user));
    }
}
