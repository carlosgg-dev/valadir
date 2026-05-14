package com.valadir.persistence.adapter;

import com.valadir.application.port.out.AccountRepository;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.repository.AccountJpaRepository;

import java.util.Optional;

public class AccountRepositoryJpaAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryJpaAdapter(AccountJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {

        return jpaRepository.findById(accountId.value())
            .map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(Email email) {

        return jpaRepository.findByEmail(email.value())
            .map(AccountMapper::toDomain);
    }

    @Override
    public void activate(AccountId accountId) {

        jpaRepository.updateStatusById(accountId.value(), AccountStatus.ACTIVE);
    }
}
