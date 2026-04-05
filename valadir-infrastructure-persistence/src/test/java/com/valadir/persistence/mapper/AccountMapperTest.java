package com.valadir.persistence.mapper;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;
import com.valadir.persistence.entity.AccountEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    @Test
    void toDomain_validEntity_mapsAllFields() {

        final var id = UUID.randomUUID();
        final var email = "bruce.wayne@email.com";
        final var hashedPassword = "$2a$12$hashedpassword";
        final var entity = new AccountEntity(id, email, hashedPassword, Role.USER);

        final Account result = AccountMapper.toDomain(entity);

        assertThat(result.getId().value()).isEqualTo(id);
        assertThat(result.getEmail().value()).isEqualTo(email);
        assertThat(result.getPassword().value()).isEqualTo(hashedPassword);
        assertThat(result.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void toEntity_validDomain_mapsAllFields() {

        final var id = UUID.randomUUID();
        final var email = "bruce.wayne@email.com";
        final var hashedPassword = "$2a$12$hashedpassword";

        final var account = Account.from(
            AccountId.from(id),
            new Email(email),
            new HashedPassword(hashedPassword),
            Role.USER
        );

        final AccountEntity result = AccountMapper.toEntity(account);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getHashedPassword()).isEqualTo(hashedPassword);
        assertThat(result.getRole()).isEqualTo(Role.USER);
    }

}
