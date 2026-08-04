package com.valadir.persistence.adapter;

import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.repository.AccountJpaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class ExpiredPendingActivationAccountCleanerJpaAdapter implements ExpiredPendingActivationAccountCleaner {

    private final AccountJpaRepository accountJpaRepository;

    public ExpiredPendingActivationAccountCleanerJpaAdapter(AccountJpaRepository accountJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
    }

    // @Modifying queries require an active transaction; Spring Data does not provide one for them
    @Override
    @Transactional
    public int delete(Instant cutoff) {

        try {
            // users rows are removed automatically via ON DELETE CASCADE on users.account_id
            return accountJpaRepository.deleteByStatusOlderThan(AccountStatus.PENDING_ACTIVATION, cutoff);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — expired pending account purge failed", e);
        }
    }
}
