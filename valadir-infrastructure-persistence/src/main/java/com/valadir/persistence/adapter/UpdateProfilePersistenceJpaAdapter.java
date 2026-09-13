package com.valadir.persistence.adapter;

import com.valadir.application.port.out.UpdateProfilePersistence;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Account;
import com.valadir.domain.model.User;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

public class UpdateProfilePersistenceJpaAdapter implements UpdateProfilePersistence {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public UpdateProfilePersistenceJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    // Both tables in one transaction: a failure writing the names rolls back the language, so a 503 leaves the profile as it was
    @Override
    @Transactional
    public void update(Account account, User user) {

        try {
            accountJpaRepository.updateLanguageById(account.getId().value(), account.getLanguage());
            userJpaRepository.updateNamesByAccountId(
                user.getAccountId().value(),
                user.getFullName().value(),
                user.getGivenName().value()
            );
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — profile update failed for accountId: " + account.getId().value(), e);
        }
    }
}
