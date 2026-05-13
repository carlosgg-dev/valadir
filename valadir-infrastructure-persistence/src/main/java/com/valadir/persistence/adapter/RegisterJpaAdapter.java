package com.valadir.persistence.adapter;

import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.transaction.annotation.Transactional;

public class RegisterJpaAdapter implements RegisterPersistence {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public RegisterJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public void save(Account account, User user) {

        accountJpaRepository.save(AccountMapper.toEntity(account));
        userJpaRepository.save(UserMapper.toEntity(user));
    }

    @Override
    @Transactional
    public void replace(AccountId existingAccountId, Account newAccount, User newUser) {

        userJpaRepository.deleteByAccountId(existingAccountId.value());
        accountJpaRepository.deleteById(existingAccountId.value());

        accountJpaRepository.flush(); // force DELETE before INSERT to avoid unique constraint violation on email

        accountJpaRepository.save(AccountMapper.toEntity(newAccount));
        userJpaRepository.save(UserMapper.toEntity(newUser));
    }
}
