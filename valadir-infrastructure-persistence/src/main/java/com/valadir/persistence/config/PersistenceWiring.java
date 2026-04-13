package com.valadir.persistence.config;

import com.valadir.application.port.out.AccountRepository;
import com.valadir.application.port.out.RegisterPersistence;
import com.valadir.persistence.adapter.AccountJpaAdapter;
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
}
