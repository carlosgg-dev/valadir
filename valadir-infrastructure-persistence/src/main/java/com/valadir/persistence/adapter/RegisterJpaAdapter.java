package com.valadir.persistence.adapter;

import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.User;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;

public class RegisterJpaAdapter implements RegisterPersistence {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public RegisterJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public void save(Account account, User user) {

        accountJpaRepository.save(AccountMapper.toEntity(account));
        userJpaRepository.save(UserMapper.toEntity(user));
    }
}
