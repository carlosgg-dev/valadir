package com.valadir.domain.model;

public class Account {

    private final AccountId id;
    private final Email email;
    private final HashedPassword password;
    private final Role role;

    private Account(AccountId id, Email email, HashedPassword password, Role role) {

        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public static Account from(AccountId id, Email email, HashedPassword password, Role role) {

        return new Account(id, email, password, role);
    }

    public static Account reconstitute(AccountId id, Email email, HashedPassword hashedPassword, Role role) {

        return new Account(id, email, hashedPassword, role);
    }

    public AccountId getId() {

        return id;
    }

    public Email getEmail() {

        return email;
    }

    public HashedPassword getPassword() {

        return password;
    }

    public Role getRole() {

        return role;
    }
}
