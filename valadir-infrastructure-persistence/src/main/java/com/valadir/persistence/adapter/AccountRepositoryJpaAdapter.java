package com.valadir.persistence.adapter;

import com.valadir.application.port.out.AccountRepository;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public class AccountRepositoryJpaAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryJpaAdapter(AccountJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {

        try {
            return jpaRepository.findById(accountId.value())
                .map(AccountMapper::toDomain);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — account lookup failed for accountId: " + accountId.value(), e);
        }
    }

    @Override
    public Optional<Account> findByEmail(Email email) {

        try {
            return jpaRepository.findByEmail(email.value())
                .map(AccountMapper::toDomain);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — account lookup by email failed", e);
        }
    }

    // @Modifying queries require an active transaction; Spring Data does not provide one for them
    @Override
    @Transactional
    public void activate(AccountId accountId) {

        try {
            jpaRepository.updateStatusById(accountId.value(), AccountStatus.ACTIVE);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — account activation failed for accountId: " + accountId.value(), e);
        }
    }

    // @Modifying queries require an active transaction; Spring Data does not provide one for them
    @Override
    @Transactional
    public void updatePassword(AccountId accountId, HashedPassword hashedPassword) {

        try {
            jpaRepository.updatePasswordById(accountId.value(), hashedPassword.value());
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — password update failed for accountId: " + accountId.value(), e);
        }
    }
}
