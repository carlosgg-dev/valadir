package com.valadir.persistence.adapter;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import com.valadir.persistence.PostgresTestContainer;
import com.valadir.persistence.repository.AccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AccountJpaAdapterTest extends PostgresTestContainer {

    @Autowired
    private AccountJpaRepository jpaRepository;

    private AccountJpaAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new AccountJpaAdapter(jpaRepository);
    }

    @Test
    void findById_existingAccount_returnsAccount() {

        Account account = buildAccount();
        adapter.save(account);

        Optional<Account> result = adapter.findById(account.getId());

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(account.getId());
        assertThat(retrieved.getEmail()).isEqualTo(account.getEmail());
        assertThat(retrieved.getPassword()).isEqualTo(account.getPassword());
        assertThat(retrieved.getRole()).isEqualTo(account.getRole());
    }

    @Test
    void findById_nonExistingAccount_returnsEmpty() {

        Optional<Account> result = adapter.findById(AccountId.generate());

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_existingAccount_returnsAccount() {

        Account account = buildAccount();
        adapter.save(account);

        Optional<Account> result = adapter.findByEmail(account.getEmail());

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(account.getId());
        assertThat(retrieved.getEmail()).isEqualTo(account.getEmail());
        assertThat(retrieved.getPassword()).isEqualTo(account.getPassword());
        assertThat(retrieved.getRole()).isEqualTo(account.getRole());
    }

    @Test
    void findByEmail_nonExistingAccount_returnsEmpty() {

        Optional<Account> result = adapter.findByEmail(new Email("unknown@email.com"));

        assertThat(result).isEmpty();
    }

    @Test
    void save_validAccount_persistsToDatabase() {

        Account account = buildAccount();

        adapter.save(account);

        assertThat(jpaRepository.findByEmail(account.getEmail().value())).isPresent();
    }

    private Account buildAccount() {

        return Account.from(
            AccountId.generate(),
            new Email("bruce.wayne@email.com"),
            new HashedPassword("$2a$12$hashedpassword"),
            Role.USER
        );
    }
}
