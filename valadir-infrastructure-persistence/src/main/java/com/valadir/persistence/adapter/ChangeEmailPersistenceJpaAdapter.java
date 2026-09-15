package com.valadir.persistence.adapter;

import com.valadir.application.exception.ApplicationException;
import com.valadir.application.port.out.ChangeEmailPersistence;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

public class ChangeEmailPersistenceJpaAdapter implements ChangeEmailPersistence {

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public ChangeEmailPersistenceJpaAdapter(AccountJpaRepository accountJpaRepository, UserJpaRepository userJpaRepository) {

        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    // @Modifying queries require an active transaction; Spring Data does not provide one for them
    @Override
    @Transactional
    public void change(AccountId accountId, Email newEmail) {

        updateEmail(accountId, newEmail);
    }

    // One transaction: a failed update restores the abandoned account, so a 503 leaves both accounts as they were
    @Override
    @Transactional
    public void changeReplacing(AccountId abandonedAccountId, AccountId accountId, Email newEmail) {

        try {
            userJpaRepository.deleteByAccountId(abandonedAccountId.value());
            accountJpaRepository.deleteById(abandonedAccountId.value());

            // force DELETE before UPDATE to avoid unique constraint violation on email
            accountJpaRepository.flush();
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — abandoned account removal failed", e);
        }

        updateEmail(accountId, newEmail);
    }

    private void updateEmail(AccountId accountId, Email newEmail) {

        try {
            accountJpaRepository.updateEmailById(accountId.value(), newEmail.value());
        } catch (DataIntegrityViolationException e) {
            // Activated by another account after the holder check: a business conflict (409), not an outage
            throw new ApplicationException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS, e);
        } catch (DataAccessException e) {
            throw new InfrastructureException("Postgres unavailable — email update failed for accountId: " + accountId.value(), e);
        }
    }
}
