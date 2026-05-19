package com.valadir.persistence.adapter;

import com.valadir.application.port.out.UserRepository;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.UserJpaRepository;

import java.util.Optional;

public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryJpaAdapter(UserJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findByAccountId(AccountId accountId) {

        return jpaRepository.findByAccountId(accountId.value())
            .map(UserMapper::toDomain);
    }
}
