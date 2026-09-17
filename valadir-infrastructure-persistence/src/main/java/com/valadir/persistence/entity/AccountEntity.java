package com.valadir.persistence.entity;

import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Language language;

    protected AccountEntity() {

    }

    public AccountEntity(UUID id, String email, String hashedPassword, Role role, AccountStatus status, Language language) {

        this.id = id;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.role = role;
        this.status = status;
        this.language = language;
    }

    public UUID getId() {

        return id;
    }

    public String getEmail() {

        return email;
    }

    public String getHashedPassword() {

        return hashedPassword;
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
