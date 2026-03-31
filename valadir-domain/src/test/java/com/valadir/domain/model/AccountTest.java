package com.valadir.domain.model;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.service.PasswordSecurityService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private final PasswordSecurityService securityService = new PasswordSecurityService();

    @Test
    void shouldCreateAccount_WhenDataIsValid() {

        AccountId id = AccountId.generate();
        Email email = new Email("bruce.wayne@email.com");
        RawPassword rawPassword = new RawPassword("SecureP@ss123");
        HashedPassword hashedPassword = new HashedPassword("$2a$12$hashedpassword");
        Role role = Role.USER;
        UserProfileData profileData = new UserProfileData(new FullName("Bruce Wayne"), new GivenName("Batman"));

        Account account = Account.createWithProfileSafety(id, email, rawPassword, hashedPassword, role, profileData, securityService);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getPassword()).isEqualTo(hashedPassword);
        assertThat(account.getRole()).isEqualTo(role);
    }

    @Test
    void shouldThrowException_WhenPasswordContainsPersonalData() {

        AccountId id = AccountId.generate();
        Email email = new Email("bruce.wayne@email.com");
        RawPassword rawPassword = new RawPassword("Bruce@2026!");
        HashedPassword hashedPassword = new HashedPassword("$2a$12$hashedpassword");
        Role role = Role.USER;
        UserProfileData profileData = new UserProfileData(new FullName("Bruce Wayne"), new GivenName("Bruce"));

        assertThatThrownBy(() -> Account.createWithProfileSafety(id, email, rawPassword, hashedPassword, role, profileData, securityService))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSECURE_PASSWORD);
    }

    @Test
    void shouldReconstituteAccount() {

        AccountId id = AccountId.generate();
        Email email = new Email("bruce.wayne@email.com");
        HashedPassword hashedPassword = new HashedPassword("$2a$12$hashedpassword");
        Role role = Role.ADMIN;

        Account account = Account.reconstitute(id, email, hashedPassword, role);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getPassword()).isEqualTo(hashedPassword);
        assertThat(account.getRole()).isEqualTo(role);
    }
}
