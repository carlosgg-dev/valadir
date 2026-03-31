package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.UserProfileData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PasswordSecurityServiceTest {

    private final PasswordSecurityService securityService = new PasswordSecurityService();

    @Test
    void shouldPass_WhenPasswordIsSecure() {

        RawPassword password = new RawPassword("Secure_P@ss_2026");
        Email email = new Email("bruce.wayne@email.com");
        FullName fullName = new FullName("Bruce Wayne");
        GivenName givenName = new GivenName("Batman");
        UserProfileData userProfileData = new UserProfileData(fullName, givenName);

        assertDoesNotThrow(() -> securityService.validatePassword(password, email, userProfileData));
    }

    @Test
    void shouldPass_WhenNameTermsAreBelowMinLength() {

        // Terms "jo", "li", "ann" are all < MIN_TERM_LENGTH (4) — they are ignored during validation
        RawPassword password = new RawPassword("Xk9@Secure1");
        Email email = new Email("jo@example.com");
        FullName fullName = new FullName("Jo Li");
        GivenName givenName = new GivenName("Ann");
        UserProfileData userProfileData = new UserProfileData(fullName, givenName);

        assertDoesNotThrow(() -> securityService.validatePassword(password, email, userProfileData));
    }

    @Test
    void shouldThrowException_WhenPasswordContainsTheEmail() {

        assertInsecurePassword("bruce.wayne@email.com1A", "bruce.wayne@email.com", "Bruce Wayne", "Batman");
    }

    @Test
    void shouldThrowException_WhenPasswordContainsFullNameWithDotSeparator() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Bruce.Wayne", "Batman");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Bruce.Wayne", "Batman");
        assertInsecurePassword("Bruce-wayne@2026", "brucewayne@email.com", "Bruce.Wayne", "Batman");
    }

    @Test
    void shouldThrowException_WhenPasswordContainsFullNameWithDashSeparator() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Bruce-Wayne", "Batman");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Bruce-Wayne", "Batman");
        assertInsecurePassword("Bruce-wayne@2026", "brucewayne@email.com", "Bruce-Wayne", "Batman");
    }

    @Test
    void shouldThrowException_WhenPasswordContainsFullNameWithUnderscoreSeparator() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Bruce_Wayne", "Batman");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Bruce_Wayne", "Batman");
        assertInsecurePassword("Bruce_wayne@2026", "brucewayne@email.com", "Bruce_Wayne", "Batman");
    }

    @Test
    void shouldThrowException_WhenPasswordContainsGivenName() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Batman", "Bruce Wayne");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Batman", "Bruce Wayne");
    }

    private void assertInsecurePassword(String pwd, String email, String fullName, String givenName) {

        RawPassword password = new RawPassword(pwd);
        Email userEmail = new Email(email);
        FullName userFullName = new FullName(fullName);
        GivenName userGivenName = new GivenName(givenName);
        UserProfileData userProfileData = new UserProfileData(userFullName, userGivenName);

        assertThatThrownBy(() -> securityService.validatePassword(password, userEmail, userProfileData))
            .isInstanceOf(DomainException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSECURE_PASSWORD);
    }
}
