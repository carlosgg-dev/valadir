package com.valadir.persistence.adapter;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(PostgresContainerConfig.class)
class ExpiredPendingActivationAccountCleanerJpaAdapterTest {

    private static final Instant CUTOFF = Instant.now().minus(72, ChronoUnit.HOURS);
    private static final Instant EXPIRED_CREATED_AT = CUTOFF.minus(8, ChronoUnit.HOURS);
    private static final Instant RECENT_CREATED_AT = CUTOFF.plus(24, ChronoUnit.HOURS);

    @Autowired
    private AccountJpaRepository accountJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExpiredPendingActivationAccountCleanerJpaAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new ExpiredPendingActivationAccountCleanerJpaAdapter(accountJpaRepository);
    }

    @Test
    void delete_pendingActivationAccount_deletesAccountAndUser() {

        AccountId accountId = savePendingActivationAccountAndUser("expired@email.com", EXPIRED_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isEqualTo(1);
        assertThat(accountJpaRepository.findById(accountId.value())).isEmpty();
        assertThat(userJpaRepository.findAll()).noneMatch(user -> user.getAccountId().equals(accountId.value()));
    }

    @Test
    void delete_recentPendingActivationAccount_doesNotDelete() {

        AccountId accountId = savePendingActivationAccountAndUser("recent@email.com", RECENT_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isZero();
        assertThat(accountJpaRepository.findById(accountId.value())).isPresent();
    }

    @Test
    void delete_activeAccount_doesNotDeleteEvenIfOld() {

        var account = AccountMother.active().build();
        var entity = AccountMapper.toEntity(account);
        var saved = accountJpaRepository.saveAndFlush(entity);

        var savedAccountId = AccountId.from(saved.getId());
        forceCreatedAt(savedAccountId, EXPIRED_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isZero();
        assertThat(accountJpaRepository.findById(savedAccountId.value())).isPresent();
    }

    @Test
    void delete_noAccounts_returnsZero() {

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isZero();
    }

    @Test
    void delete_multipleAccounts_deletesAll() {

        savePendingActivationAccountAndUser("expired1@email.com", EXPIRED_CREATED_AT);
        savePendingActivationAccountAndUser("expired2@email.com", EXPIRED_CREATED_AT);
        AccountId recentId = savePendingActivationAccountAndUser("recent@email.com", RECENT_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isEqualTo(2);
        assertThat(accountJpaRepository.findAll()).hasSize(1);
        assertThat(accountJpaRepository.findById(recentId.value())).isPresent();
    }

    private AccountId savePendingActivationAccountAndUser(String email, Instant createdAt) {

        var account = AccountMother.pendingActivation().withEmail(Email.from(email)).build();
        var accountEntity = AccountMapper.toEntity(account);
        var savedAccount = accountJpaRepository.saveAndFlush(accountEntity);
        var savedAccountId = AccountId.from(savedAccount.getId());
        forceCreatedAt(savedAccountId, createdAt);

        var user = UserMother.builder().withAccountId(savedAccountId).build();
        var userEntity = UserMapper.toEntity(user);
        userJpaRepository.saveAndFlush(userEntity);

        return savedAccountId;
    }

    private void forceCreatedAt(AccountId accountId, Instant createdAt) {

        jdbcTemplate.update(
            "UPDATE accounts SET created_at = ? WHERE id = ?",
            Timestamp.from(createdAt),
            accountId.value()
        );
    }
}
