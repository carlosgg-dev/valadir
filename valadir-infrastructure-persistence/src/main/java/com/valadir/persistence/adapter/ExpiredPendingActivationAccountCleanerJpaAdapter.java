package com.valadir.persistence.adapter;

import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class ExpiredPendingActivationAccountCleanerJpaAdapter implements ExpiredPendingActivationAccountCleaner {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public ExpiredPendingActivationAccountCleanerJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    // @Modifying queries require an active transaction; Spring Data does not provide one for them
    @Override
    @Transactional
    public int delete(Instant cutoff) {

        try {
            // Profiles go first: the schema's RESTRICT would refuse an account that still has one
            userJpaRepository.deleteByAccountStatusOlderThan(AccountStatus.PENDING_ACTIVATION, cutoff);
            return accountJpaRepository.deleteByStatusOlderThan(AccountStatus.PENDING_ACTIVATION, cutoff);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — expired pending account purge failed", e);
        }
    }
}
