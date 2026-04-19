package com.valadir.persistence.mapper;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
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

        var id = UUID.randomUUID();
        var email = "bruce.wayne@email.com";
        var hashedPassword = "$2a$12$hashedpassword";
        var entity = new AccountEntity(id, email, hashedPassword, Role.USER, AccountStatus.ACTIVE);

        Account result = AccountMapper.toDomain(entity);

        assertThat(result.getId().value()).isEqualTo(id);
        assertThat(result.getEmail().value()).isEqualTo(email);
        assertThat(result.getPassword().value()).isEqualTo(hashedPassword);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void toEntity_validDomain_mapsAllFields() {

        var id = UUID.randomUUID();
        var email = "bruce.wayne@email.com";
        var hashedPassword = "$2a$12$hashedpassword";

        var account = Account.reconstitute(
            AccountId.from(id),
            new Email(email),
            new HashedPassword(hashedPassword),
            Role.USER,
            AccountStatus.ACTIVE
        );

        AccountEntity result = AccountMapper.toEntity(account);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getHashedPassword()).isEqualTo(hashedPassword);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
