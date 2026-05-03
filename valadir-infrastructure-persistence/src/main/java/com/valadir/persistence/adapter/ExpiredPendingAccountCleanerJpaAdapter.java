package com.valadir.persistence.adapter;

import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class ExpiredPendingAccountCleanerJpaAdapter implements ExpiredPendingAccountCleaner {

    private final UserJpaRepository userJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    public ExpiredPendingAccountCleanerJpaAdapter(UserJpaRepository userJpaRepository, AccountJpaRepository accountJpaRepository) {

        this.userJpaRepository = userJpaRepository;
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    @Transactional
    public int delete(Instant cutoff) {

        userJpaRepository.deleteExpiredPendingVerifications(cutoff);
        return accountJpaRepository.deleteExpiredPendingVerification(cutoff);
    }
}
