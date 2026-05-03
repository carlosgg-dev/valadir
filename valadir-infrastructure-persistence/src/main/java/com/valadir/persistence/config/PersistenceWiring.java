package com.valadir.persistence.config;

import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.ExpiredPendingAccountCleaner;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.persistence.adapter.AccountJpaAdapter;
import com.valadir.persistence.adapter.ExpiredPendingAccountCleanerJpaAdapter;
import com.valadir.persistence.adapter.RegisterJpaAdapter;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PersistenceWiring {

    @Bean
    AccountRepository accountRepository(AccountJpaRepository accountJpaRepository) {

        return new AccountJpaAdapter(accountJpaRepository);
    }

    @Bean
    RegisterPersistence registerPersistence(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        return new RegisterJpaAdapter(accountJpaRepository, userJpaRepository);
    }

    @Bean
    ExpiredPendingAccountCleaner expiredPendingAccountCleaner(UserJpaRepository userJpaRepository, AccountJpaRepository accountJpaRepository) {

        return new ExpiredPendingAccountCleanerJpaAdapter(userJpaRepository, accountJpaRepository);
    }
}
