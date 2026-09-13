package com.valadir.persistence.adapter;

import com.valadir.application.port.out.DeleteAccountPersistence;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

public class DeleteAccountPersistenceJpaAdapter implements DeleteAccountPersistence {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public DeleteAccountPersistenceJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public void delete(AccountId accountId) {

        try {
            // The profile is deleted explicitly, before its account: the schema's cascade guards integrity,
            // it does not decide what a deletion removes.
            userJpaRepository.deleteByAccountId(accountId.value());
            accountJpaRepository.deleteById(accountId.value());

            // Left to the commit, a failing DELETE would be raised outside this catch
            accountJpaRepository.flush();
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — account deletion failed for accountId: " + accountId.value(), e);
        }
    }
}
