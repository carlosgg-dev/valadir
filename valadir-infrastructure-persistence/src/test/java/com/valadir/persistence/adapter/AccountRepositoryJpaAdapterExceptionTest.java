package com.valadir.persistence.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.persistence.repository.AccountJpaRepository;
import com.valadir.test.mother.PasswordMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryJpaAdapterExceptionTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    private static final AccountId ACCOUNT_ID = AccountId.from(UUID.randomUUID());
    private static final Email EMAIL = Email.from("bruce@wayne.com");
    private static final HashedPassword HASHED_PASSWORD = PasswordMother.hashed();

    @Mock
    private AccountJpaRepository jpaRepository;

    @InjectMocks
    private AccountRepositoryJpaAdapter adapter;

    @Test
    void findById_postgresError_throwsInfrastructureException() {

        given(jpaRepository.findById(any(UUID.class))).willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.findById(ACCOUNT_ID))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void findByEmail_postgresError_throwsInfrastructureException() {

        given(jpaRepository.findByEmail(anyString())).willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.findByEmail(EMAIL))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void activate_postgresError_throwsInfrastructureException() {

        willThrow(POSTGRES_ERROR).given(jpaRepository).updateStatusById(any(UUID.class), any(AccountStatus.class));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.activate(ACCOUNT_ID))
            .withCause(POSTGRES_ERROR);
    }

    @Test
    void updatePassword_postgresError_throwsInfrastructureException() {

        willThrow(POSTGRES_ERROR).given(jpaRepository).updatePasswordById(any(UUID.class), anyString());

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.updatePassword(ACCOUNT_ID, HASHED_PASSWORD))
            .withCause(POSTGRES_ERROR);
    }
}
