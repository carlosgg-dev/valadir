package com.valadir.persistence.adapter;

import com.valadir.application.port.out.UserRepository;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataAccessException;

import java.util.Optional;

public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryJpaAdapter(UserJpaRepository jpaRepository) {

        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findByAccountId(AccountId accountId) {

        try {
            return jpaRepository.findByAccountId(accountId.value())
                .map(UserMapper::toDomain);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — user lookup failed for accountId: " + accountId.value(), e);
        }
    }
}
