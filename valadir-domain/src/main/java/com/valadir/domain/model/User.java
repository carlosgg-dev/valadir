package com.valadir.domain.model;

public class User {

    private final UserId id;
    private final AccountId accountId;
    private final FullName fullName;
    private final GivenName givenName;

    private User(UserId id, AccountId accountId, FullName fullName, GivenName givenName) {

        this.id = id;
        this.accountId = accountId;
        this.fullName = fullName;
        this.givenName = givenName;
    }

    public static User newProfile(UserId id, AccountId accountId, FullName fullName, GivenName givenName) {

        return new User(id, accountId, fullName, givenName);
    }

    public static User reconstitute(UserId id, AccountId accountId, FullName fullName, GivenName givenName) {

        return new User(id, accountId, fullName, givenName);
    }

    public UserId getId() {

        return id;
    }

    public AccountId getAccountId() {

        return accountId;
    }

    public FullName getFullName() {

        return fullName;
    }

    public GivenName getGivenName() {

        return givenName;
    }
}
