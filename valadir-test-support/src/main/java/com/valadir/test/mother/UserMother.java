package com.valadir.test.mother;

import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;

public final class UserMother {

    private UserMother() {

    }

    public static Builder builder() {

        return new Builder()
            .withId(UserId.generate())
            .withAccountId(AccountId.generate())
            .withFullName(FullName.from("Bruce Wayne"))
            .withGivenName(GivenName.from("Bruce"));
    }

    public static final class Builder {

        private UserId id;
        private AccountId accountId;
        private FullName fullName;
        private GivenName givenName;

        private Builder() {

        }

        public Builder withId(UserId id) {

            this.id = id;
            return this;
        }

        public Builder withAccountId(AccountId accountId) {

            this.accountId = accountId;
            return this;
        }

        public Builder withFullName(FullName fullName) {

            this.fullName = fullName;
            return this;
        }

        public Builder withGivenName(GivenName givenName) {

            this.givenName = givenName;
            return this;
        }

        public User build() {

            return User.reconstitute(id, accountId, fullName, givenName);
        }
    }
}
