package com.valadir.persistence.entity;

import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_role")
    private Role role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "account_status")
    private AccountStatus status;

    protected AccountEntity() {

    }

    public AccountEntity(UUID id, String email, String hashedPassword, Role role, AccountStatus status) {

        this.id = id;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.role = role;
        this.status = status;
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
}
