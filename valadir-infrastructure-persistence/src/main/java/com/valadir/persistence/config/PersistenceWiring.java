package com.valadir.persistence.config;

import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.ExpiredPendingActivationAccountCleaner;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.persistence.adapter.AccountRepositoryJpaAdapter;
import com.valadir.persistence.adapter.ExpiredPendingActivationAccountCleanerJpaAdapter;
import com.valadir.persistence.adapter.RegisterPersistenceJpaAdapter;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PersistenceWiring {

    @Bean
    AccountRepository accountRepository(AccountJpaRepository accountJpaRepository) {

        return new AccountRepositoryJpaAdapter(accountJpaRepository);
    }

    @Bean
    RegisterPersistence registerPersistence(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        return new RegisterPersistenceJpaAdapter(accountJpaRepository, userJpaRepository);
    }

    @Bean
    ExpiredPendingActivationAccountCleaner expiredPendingActivationAccountCleaner(AccountJpaRepository accountJpaRepository) {

        return new ExpiredPendingActivationAccountCleanerJpaAdapter(accountJpaRepository);
    }
}
