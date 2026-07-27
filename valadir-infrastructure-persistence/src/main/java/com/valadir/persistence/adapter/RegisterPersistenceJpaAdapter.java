package com.valadir.persistence.adapter;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.User;
import com.valadir.persistence.mapper.AccountMapper;
import com.valadir.persistence.mapper.UserMapper;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

public class RegisterPersistenceJpaAdapter implements RegisterPersistence {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public RegisterPersistenceJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public void save(Account account, User user) {

        insertAccount(account);
        userJpaRepository.save(UserMapper.toEntity(user));
    }

    @Override
    @Transactional
    public void replace(AccountId existingAccountId, Account newAccount, User newUser) {

        userJpaRepository.deleteByAccountId(existingAccountId.value());
        accountJpaRepository.deleteById(existingAccountId.value());

        // force DELETE before INSERT to avoid unique constraint violation on email
        accountJpaRepository.flush();

        insertAccount(newAccount);
        userJpaRepository.save(UserMapper.toEntity(newUser));
    }

    // saveAndFlush: with an assigned id the INSERT would be deferred to the commit, outside the catch.
    // Email is the only constraint a concurrent registration can hit — ids are generated UUIDs.
    private void insertAccount(Account account) {

        try {
            accountJpaRepository.saveAndFlush(AccountMapper.toEntity(account));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS, e);
        }
    }
}
