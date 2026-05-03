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
import com.valadir.persistence.entity.AccountEntity;
import com.valadir.persistence.entity.UserEntity;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpiredPendingAccountCleanerJpaAdapterTest extends PostgresTestContainer {

    private static final Instant CUTOFF = Instant.now().minus(72, ChronoUnit.HOURS);
    private static final Instant EXPIRED_CREATED_AT = CUTOFF.minus(8, ChronoUnit.HOURS);
    private static final Instant RECENT_CREATED_AT = CUTOFF.plus(24, ChronoUnit.HOURS);

    @Autowired
    private AccountJpaRepository accountJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExpiredPendingAccountCleanerJpaAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new ExpiredPendingAccountCleanerJpaAdapter(userJpaRepository, accountJpaRepository);
    }

    @Test
    void delete_pendingAccount_deletesAccountAndUser() {

        AccountId accountId = savePendingAccountAndUser("expired@email.com", EXPIRED_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isEqualTo(1);
        assertThat(accountJpaRepository.findById(accountId.value())).isEmpty();
        assertThat(userJpaRepository.findAll()).noneMatch(user -> user.getAccountId().equals(accountId.value()));
    }

    @Test
    void delete_recentPendingAccount_doesNotDelete() {

        AccountId accountId = savePendingAccountAndUser("recent@email.com", RECENT_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isZero();
        assertThat(accountJpaRepository.findById(accountId.value())).isPresent();
    }

    @Test
    void delete_activeAccount_doesNotDeleteEvenIfOld() {

        AccountId accountId = AccountId.generate();
        Account account = Account.reconstitute(
            accountId,
            new Email("active@email.com"),
            new HashedPassword("$2a$12$hash"),
            Role.USER,
            AccountStatus.ACTIVE
        );
        AccountEntity entity = AccountMapper.toEntity(account);

        accountJpaRepository.saveAndFlush(entity);
        forceCreatedAt(accountId, EXPIRED_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isZero();
        assertThat(accountJpaRepository.findById(accountId.value())).isPresent();
    }

    @Test
    void delete_noAccounts_returnsZero() {

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isZero();
    }

    @Test
    void delete_multipleAccounts_deletesAll() {

        savePendingAccountAndUser("expired1@email.com", EXPIRED_CREATED_AT);
        savePendingAccountAndUser("expired2@email.com", EXPIRED_CREATED_AT);
        AccountId recentId = savePendingAccountAndUser("recent@email.com", RECENT_CREATED_AT);

        int deleted = adapter.delete(CUTOFF);

        assertThat(deleted).isEqualTo(2);
        assertThat(accountJpaRepository.findAll()).hasSize(1);
        assertThat(accountJpaRepository.findById(recentId.value())).isPresent();
    }

    private AccountId savePendingAccountAndUser(String email, Instant createdAt) {

        AccountId accountId = AccountId.generate();
        Account account = Account.newPendingVerification(accountId, new Email(email), new HashedPassword("$2a$12$hash"), Role.USER);
        AccountEntity accountEntity = AccountMapper.toEntity(account);

        accountJpaRepository.saveAndFlush(accountEntity);
        forceCreatedAt(accountId, createdAt);

        User user = User.newProfile(UserId.generate(), accountId, new FullName("Test User"), new GivenName("Test"));
        UserEntity userEntity = UserMapper.toEntity(user);
        userJpaRepository.saveAndFlush(userEntity);

        return accountId;
    }

    private void forceCreatedAt(AccountId accountId, Instant createdAt) {

        jdbcTemplate.update(
            "UPDATE accounts SET created_at = ? WHERE id = ?",
            Timestamp.from(createdAt),
            accountId.value()
        );
    }
}
