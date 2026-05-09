package com.valadir.persistence.adapter;

import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.repository.AccountJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class ExpiredPendingAccountCleanerJpaAdapter implements ExpiredPendingAccountCleaner {

    private final AccountJpaRepository accountJpaRepository;

    public ExpiredPendingAccountCleanerJpaAdapter(AccountJpaRepository accountJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    @Transactional
    public int delete(Instant cutoff) {

        // users rows are removed automatically via ON DELETE CASCADE on users.account_id
        return accountJpaRepository.deleteByStatusOlderThan(AccountStatus.PENDING_VERIFICATION, cutoff);
    }
}
