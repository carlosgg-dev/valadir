package com.valadir.persistence.mapper;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toDomain_validEntity_mapsAllFields() {

        final var id = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var fullName = "Bruce Wayne";
        final var givenName = "Bruce";

        final var entity = new UserEntity(id, accountId, fullName, givenName);

        final User result = UserMapper.toDomain(entity);

        assertThat(result.getId().value()).isEqualTo(id);
        assertThat(result.getAccountId().value()).isEqualTo(accountId);
        assertThat(result.getFullName().value()).isEqualTo(fullName);
        assertThat(result.getGivenName().value()).isEqualTo(givenName);
    }

    @Test
    void toEntity_validDomain_mapsAllFields() {

        final var id = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var fullName = "Bruce Wayne";
        final var givenName = "Bruce";

        final User user = User.newProfile(
            UserId.from(id),
            AccountId.from(accountId),
            new FullName(fullName),
            new GivenName(givenName)
        );

        final UserEntity result = UserMapper.toEntity(user);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getFullName()).isEqualTo(fullName);
        assertThat(result.getGivenName()).isEqualTo(givenName);
    }

}
