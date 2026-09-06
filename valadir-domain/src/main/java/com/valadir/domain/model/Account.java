package com.valadir.domain.model;

public class Account {

    private final AccountId id;
    private final Email email;
    private final HashedPassword password;
    private final Role role;
    private final AccountStatus status;
    private final Language language;

    private Account(
        AccountId id,
        Email email,
        HashedPassword password,
        Role role,
        AccountStatus status,
        Language language
    ) {

        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.language = language;
    }

    public static Account newPendingActivation(
        AccountId id,
        Email email,
        HashedPassword password,
        Role role,
        Language language
    ) {

        return new Account(id, email, password, role, AccountStatus.PENDING_ACTIVATION, language);
    }

    public static Account reconstitute(
        AccountId id,
        Email email,
        HashedPassword hashedPassword,
        Role role,
        AccountStatus status,
        Language language
    ) {

        return new Account(id, email, hashedPassword, role, status, language);
    }

    public Account activate() {

        return new Account(id, email, password, role, AccountStatus.ACTIVE, language);
    }

    public boolean isActive() {

        return AccountStatus.ACTIVE.equals(status);
    }

    public boolean isPendingActivation() {

        return AccountStatus.PENDING_ACTIVATION.equals(status);
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

    public Language getLanguage() {

        return language;
    }
}
