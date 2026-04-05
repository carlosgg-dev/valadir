package com.valadir.persistence.mapper;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import com.valadir.persistence.entity.UserEntity;

public class UserMapper {

    private UserMapper() {

    }

    public static User toDomain(UserEntity entity) {

        return User.reconstitute(
            UserId.from(entity.getId()),
            AccountId.from(entity.getAccountId()),
            new FullName(entity.getFullName()),
            new GivenName(entity.getGivenName())
        );
    }

    public static UserEntity toEntity(User user) {

        return new UserEntity(
            user.getId().value(),
            user.getAccountId().value(),
            user.getFullName().value(),
            user.getGivenName().value()
        );
    }
}
