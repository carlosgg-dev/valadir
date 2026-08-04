package com.valadir.persistence.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.AccountId;
import com.valadir.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserRepositoryJpaAdapterExceptionTest {

    private static final DataAccessException POSTGRES_ERROR = new DataAccessException("Postgres error") {
    };

    @Mock
    private UserJpaRepository jpaRepository;

    @InjectMocks
    private UserRepositoryJpaAdapter adapter;

    @Test
    void findByAccountId_postgresError_throwsInfrastructureException() {

        var accountId = AccountId.generate();
        
        given(jpaRepository.findByAccountId(any(UUID.class))).willThrow(POSTGRES_ERROR);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.findByAccountId(accountId))
            .withCause(POSTGRES_ERROR);
    }
}
