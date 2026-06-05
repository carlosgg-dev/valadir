package com.valadir.persistence.adapter;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import com.valadir.test.containers.PostgresContainerConfig;
import com.valadir.test.mother.AccountMother;
import com.valadir.test.mother.UserMother;
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
class UserRepositoryJpaAdapterTest {

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

        var account = AccountMother.active().build();
        var savedAccount = accountJpaRepository.save(AccountMapper.toEntity(account));

        var user = UserMother.builder().withAccountId(AccountId.from(savedAccount.getId())).build();
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
}
