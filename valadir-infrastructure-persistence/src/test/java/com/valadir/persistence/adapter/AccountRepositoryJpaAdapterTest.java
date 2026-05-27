package com.valadir.persistence.adapter;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.test.containers.PostgresContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(PostgresContainerConfig.class)
class AccountRepositoryJpaAdapterTest {

    @Autowired
    private AccountJpaRepository jpaRepository;

    private AccountRepositoryJpaAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new AccountRepositoryJpaAdapter(jpaRepository);
    }

    @Test
    void findById_existingAccount_returnsAccount() {

        var account = buildAccount();
        var saved = jpaRepository.save(AccountMapper.toEntity(account));

        Optional<Account> result = adapter.findById(AccountId.from(saved.getId()));

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(account.getId());
        assertThat(retrieved.getEmail()).isEqualTo(account.getEmail());
        assertThat(retrieved.getPassword()).isEqualTo(account.getPassword());
        assertThat(retrieved.getRole()).isEqualTo(account.getRole());
        assertThat(retrieved.getStatus()).isEqualTo(account.getStatus());
    }

    @Test
    void findById_nonExistingAccount_returnsEmpty() {

        Optional<Account> result = adapter.findById(AccountId.generate());

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_existingAccount_returnsAccount() {

        Account account = buildAccount();
        jpaRepository.save(AccountMapper.toEntity(account));

        Optional<Account> result = adapter.findByEmail(account.getEmail());

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(account.getId());
        assertThat(retrieved.getEmail()).isEqualTo(account.getEmail());
        assertThat(retrieved.getPassword()).isEqualTo(account.getPassword());
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

        var pendingAccount = Account.newPendingActivation(
            AccountId.generate(),
            Email.from("pending@email.com"),
            new HashedPassword("$2a$12$hashedpassword"),
            Role.USER
        );

        var saved = jpaRepository.save(AccountMapper.toEntity(pendingAccount));
        adapter.activate(AccountId.from(saved.getId()));

        var result = adapter.findById(AccountId.from(saved.getId()));

        assertThat(result)
            .isPresent()
            .hasValueSatisfying(account -> assertThat(account.isActive()).isTrue());
    }

    @Test
    void updatePassword_existingAccount_updatesHashedPassword() {

        var existingAccount = buildAccount();
        var saved = jpaRepository.save(AccountMapper.toEntity(existingAccount));

        var newHash = new HashedPassword("$argon2id$newpassword");
        adapter.updatePassword(AccountId.from(saved.getId()), newHash);

        var result = adapter.findById(AccountId.from(saved.getId()));
        assertThat(result)
            .isPresent()
            .hasValueSatisfying(account -> assertThat(account.getPassword())
                .isEqualTo(newHash)
                .isNotEqualTo(existingAccount.getPassword()));
    }

    private Account buildAccount() {

        return Account.reconstitute(
            AccountId.generate(),
            Email.from("bruce.wayne@email.com"),
            new HashedPassword("$2a$12$hashedpassword"),
            Role.USER,
            AccountStatus.ACTIVE
        );
    }
}
