package com.valadir.test.mother;

import com.valadir.domain.model.Account;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.Role;

public final class AccountMother {

    private AccountMother() {

    }

    public static Builder active() {

        return new Builder()
            .withId(AccountId.generate())
            .withEmail(Email.from("account@test.com"))
            .withPassword(PasswordMother.hashed())
            .withRole(Role.USER)
            .withStatus(AccountStatus.ACTIVE);
    }

    public static Builder pendingActivation() {

        return new Builder()
            .withId(AccountId.generate())
            .withEmail(Email.from("account@test.com"))
            .withPassword(PasswordMother.hashed())
            .withRole(Role.USER)
            .withStatus(AccountStatus.PENDING_ACTIVATION);
    }

    public static final class Builder {

        private AccountId id;
        private Email email;
        private HashedPassword password;
        private Role role;
        private AccountStatus status;

        private Builder() {

        }

        public Builder withId(AccountId id) {

            this.id = id;
            return this;
        }

        public Builder withEmail(Email email) {

            this.email = email;
            return this;
        }

        public Builder withPassword(HashedPassword password) {

            this.password = password;
            return this;
        }

        public Builder withRole(Role role) {

            this.role = role;
            return this;
        }

        public Builder withStatus(AccountStatus status) {

            this.status = status;
            return this;
        }

        public Account build() {

            return Account.reconstitute(id, email, password, role, status);
        }
    }
}
