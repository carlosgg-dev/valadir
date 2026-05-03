package com.valadir.domain.model;

public class Account {

    private final AccountId id;
    private final Email email;
    private final HashedPassword password;
    private final Role role;
    private final AccountStatus status;

    private Account(AccountId id, Email email, HashedPassword password, Role role, AccountStatus status) {

        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public static Account newPendingVerification(AccountId id, Email email, HashedPassword password, Role role) {

        return new Account(id, email, password, role, AccountStatus.PENDING_VERIFICATION);
    }

    public static Account reconstitute(AccountId id, Email email, HashedPassword hashedPassword, Role role, AccountStatus status) {

        return new Account(id, email, hashedPassword, role, status);
    }

    public Account activate() {

        return new Account(id, email, password, role, AccountStatus.ACTIVE);
    }

    public boolean isActive() {

        return AccountStatus.ACTIVE.equals(status);
    }

    public boolean isPendingVerification() {

        return AccountStatus.PENDING_VERIFICATION.equals(status);
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

    public AccountStatus getStatus() {

        return status;
    }
}
