package com.valadir.persistence.mapper;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.persistence.entity.AccountEntity;

public class AccountMapper {

    private AccountMapper() {

    }

    public static Account toDomain(AccountEntity entity) {

        return Account.reconstitute(
            AccountId.from(entity.getId()),
            new Email(entity.getEmail()),
            new HashedPassword(entity.getHashedPassword()),
            entity.getRole()
        );
    }

    public static AccountEntity toEntity(Account account) {

        return new AccountEntity(
            account.getId().value(),
            account.getEmail().value(),
            account.getPassword().value(),
            account.getRole()
        );
    }
}
