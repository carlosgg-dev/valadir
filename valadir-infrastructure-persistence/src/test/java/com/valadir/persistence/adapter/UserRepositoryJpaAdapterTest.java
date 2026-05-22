package com.valadir.persistence.adapter;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.persistence.PostgresTestContainer;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
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
class UserRepositoryJpaAdapterTest extends PostgresTestContainer {

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserRepositoryJpaAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new UserRepositoryJpaAdapter(userJpaRepository);
    }

    @Test
    void findByAccountId_existingUser_returnsUser() {

        var account = buildAccount();
        var savedAccount = accountJpaRepository.save(AccountMapper.toEntity(account));

        var user = buildUser(AccountId.from(savedAccount.getId()));
        var savedUser = userJpaRepository.save(UserMapper.toEntity(user));

        Optional<User> result = adapter.findByAccountId(AccountId.from(savedUser.getAccountId()));

        assertThat(result).isPresent();
        var retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo(user.getId());
        assertThat(retrieved.getAccountId()).isEqualTo(user.getAccountId());
        assertThat(retrieved.getFullName()).isEqualTo(user.getFullName());
        assertThat(retrieved.getGivenName()).isEqualTo(user.getGivenName());
    }

    @Test
    void findByAccountId_nonExistingUser_returnsEmpty() {

        Optional<User> result = adapter.findByAccountId(AccountId.generate());

        assertThat(result).isEmpty();
    }

    private Account buildAccount() {

        return Account.reconstitute(
            AccountId.generate(),
            new Email("bruce.wayne@example.com"),
            new HashedPassword("$argon2id$hashedpassword"),
            Role.USER,
            AccountStatus.ACTIVE
        );
    }

    private User buildUser(AccountId accountId) {

        return User.newProfile(
            UserId.generate(),
            accountId,
            new FullName("Bruce Wayne"),
            new GivenName("Batman")
        );
    }
}
