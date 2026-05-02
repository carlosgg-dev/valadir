package com.valadir.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "given_name")
    private String givenName;

    protected UserEntity() {

    }

    public UserEntity(UUID id, UUID accountId, String fullName, String givenName) {

        this.id = id;
        this.accountId = accountId;
        this.fullName = fullName;
        this.givenName = givenName;
    }

    public UUID getId() {

        return id;
    }

    public UUID getAccountId() {

        return accountId;
    }

    public String getFullName() {

        return fullName;
    }

    public String getGivenName() {

        return givenName;
    }
}
