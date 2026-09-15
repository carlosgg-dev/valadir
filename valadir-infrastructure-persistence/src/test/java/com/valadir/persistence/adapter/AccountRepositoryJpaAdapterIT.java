package com.valadir.persistence.adapter;

import com.valadir.application.port.out.AccountRepository;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Language;
import com.valadir.persistence.config.PersistenceWiring;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.test.containers.PostgresContainerConfig;
import com.valadir.test.mother.AccountMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Runs without a test-managed transaction so the adapter executes with the same
// transactional semantics as production — a missing @Transactional fails here.
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({PostgresContainerConfig.class, PersistenceWiring.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountRepositoryJpaAdapterIT {

    private static final Instant A_DAY_AGO = Instant.now().minus(Duration.ofDays(1));

    @Autowired
    private AccountJpaRepository jpaRepository;

    @Autowired
    private AccountRepository adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {

        jpaRepository.deleteAll();
    }

    @Test
    void findById_existingAccount_returnsAccount() {

        // Not the fallback language: the column defaults to EN, so an entity that stopped writing it
        // would still read back as EN and this test would pass.
        var account = AccountMother.active().withLanguage(Language.ES).build();
        var saved = jpaRepository.save(AccountMapper.toEntity(account));

        Optional<Account> result = adapter.findById(AccountId.from(saved.getId()));

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(account.getId());
        assertThat(retrieved.getEmail()).isEqualTo(account.getEmail());
        assertThat(retrieved.getHashedPassword()).isEqualTo(account.getHashedPassword());
        assertThat(retrieved.getRole()).isEqualTo(account.getRole());
        assertThat(retrieved.getStatus()).isEqualTo(account.getStatus());
        assertThat(retrieved.getLanguage()).isEqualTo(account.getLanguage());
    }

    @Test
    void findById_nonExistingAccount_returnsEmpty() {

        Optional<Account> result = adapter.findById(AccountId.generate());

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_existingAccount_returnsAccount() {

        Account account = AccountMother.active().build();
        jpaRepository.save(AccountMapper.toEntity(account));

        Optional<Account> result = adapter.findByEmail(account.getEmail());

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(account.getId());
        assertThat(retrieved.getEmail()).isEqualTo(account.getEmail());
        assertThat(retrieved.getHashedPassword()).isEqualTo(account.getHashedPassword());
        assertThat(retrieved.getRole()).isEqualTo(account.getRole());
        assertThat(retrieved.getStatus()).isEqualTo(account.getStatus());
    }

    @Test
    void findByEmail_nonExistingAccount_returnsEmpty() {

        Optional<Account> result = adapter.findByEmail(Email.from("unknown@email.com"));

        assertThat(result).isEmpty();
    }

    @Test
    void activate_pendingActivationAccount_updatesStatusToActive() {

        var pendingAccount = AccountMother.pendingActivation().build();
        var saved = jpaRepository.save(AccountMapper.toEntity(pendingAccount));
        adapter.activate(AccountId.from(saved.getId()));

        var result = adapter.findById(AccountId.from(saved.getId()));

        assertThat(result)
            .isPresent()
            .hasValueSatisfying(account -> assertThat(account.isActive()).isTrue());
    }

    @Test
    void activate_pendingActivationAccount_movesUpdatedAt() {

        var pendingAccount = AccountMother.pendingActivation().build();
        jpaRepository.save(AccountMapper.toEntity(pendingAccount));
        forceUpdatedAt(pendingAccount.getId(), A_DAY_AGO);

        adapter.activate(pendingAccount.getId());

        assertThat(jpaRepository.findById(pendingAccount.getId().value()).orElseThrow().getUpdatedAt()).isAfter(A_DAY_AGO);
    }

    @Test
    void updatePassword_existingAccount_updatesHashedPassword() {

        var existingAccount = AccountMother.active().build();
        var saved = jpaRepository.save(AccountMapper.toEntity(existingAccount));

        var newHashedPassword = new HashedPassword("$argon2id$newpassword");
        adapter.updatePassword(AccountId.from(saved.getId()), newHashedPassword);

        var result = adapter.findById(AccountId.from(saved.getId()));
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(account -> assertThat(account.getHashedPassword())
                .isEqualTo(newHashedPassword)
                .isNotEqualTo(existingAccount.getHashedPassword()));
    }

    @Test
    void updatePassword_existingAccount_movesUpdatedAt() {

        var existingAccount = AccountMother.active().build();
        jpaRepository.save(AccountMapper.toEntity(existingAccount));
        forceUpdatedAt(existingAccount.getId(), A_DAY_AGO);

        adapter.updatePassword(existingAccount.getId(), new HashedPassword("$argon2id$newpassword"));

        assertThat(jpaRepository.findById(existingAccount.getId().value()).orElseThrow().getUpdatedAt()).isAfter(A_DAY_AGO);
    }

    private void forceUpdatedAt(AccountId accountId, Instant updatedAt) {

        jdbcTemplate.update("UPDATE accounts SET updated_at = ? WHERE id = ?", Timestamp.from(updatedAt), accountId.value());
    }
}
