package com.valadir.persistence.adapter;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.persistence.PostgresTestContainer;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RegisterJpaAdapterTest extends PostgresTestContainer {

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private RegisterJpaAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new RegisterJpaAdapter(accountJpaRepository, userJpaRepository);
    }

    @Test
    void save_validAccountAndUser_persistsBoth() {

        final AccountId accountId = AccountId.generate();
        final Account account = buildAccount(accountId);
        final User user = buildUser(accountId);

        adapter.save(account, user);

        assertThat(accountJpaRepository.findById(accountId.value())).isPresent();
        assertThat(userJpaRepository.findById(user.getId().value())).isPresent();
    }

    private Account buildAccount(AccountId accountId) {

        return Account.from(
            accountId,
            new Email("bruce.wayne@email.com"),
            new HashedPassword("$2a$12$hashedpassword"),
            Role.USER
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
