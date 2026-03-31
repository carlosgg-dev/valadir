package com.valadir.domain.model;

import com.valadir.domain.service.PasswordSecurityService;

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

    /**
     * Factory method for creating a NEW account with security checks.
     */
    public static Account createWithProfileSafety(
        AccountId id,
        Email email,
        RawPassword rawPassword,
        HashedPassword hashedPassword,
        Role role,
        UserProfileData profileData,
        PasswordSecurityService securityService
    ) {

        securityService.validatePassword(rawPassword, email, profileData);
        return new Account(id, email, hashedPassword, role);
    }

    /**
     * Factory method for RECONSTITUTING an account from persistence.
     */
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
