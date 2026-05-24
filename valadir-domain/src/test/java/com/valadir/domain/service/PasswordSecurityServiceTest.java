package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.UserProfileData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PasswordSecurityServiceTest {

    private final PasswordSecurityService securityService = new PasswordSecurityService();

    @Test
    void validatePassword_securePassword_passes() {

        RawPassword password = RawPassword.from("Secure_P@ss_2026");
        Email email = Email.from("bruce.wayne@email.com");
        FullName fullName = FullName.from("Bruce Wayne");
        GivenName givenName = GivenName.from("Batman");
        UserProfileData userProfileData = UserProfileData.from(fullName, givenName);

        assertDoesNotThrow(() -> securityService.validatePassword(password, email, userProfileData));
    }

    @Test
    void validatePassword_nameTermsBelowMinLength_passes() {

        // Terms "jo", "li", "ann" are all < MIN_TERM_LENGTH (4) — they are ignored during validation
        RawPassword password = RawPassword.from("Xk9@Secure1");
        Email email = Email.from("jo@example.com");
        FullName fullName = FullName.from("Jo Li");
        GivenName givenName = GivenName.from("Ann");
        UserProfileData userProfileData = UserProfileData.from(fullName, givenName);

        assertDoesNotThrow(() -> securityService.validatePassword(password, email, userProfileData));
    }

    @Test
    void validatePassword_passwordContainsEmail_throwsDomainException() {

        assertInsecurePassword("bruce.wayne@email.com1A", "bruce.wayne@email.com", "Bruce Wayne", "Batman");
    }

    @Test
    void validatePassword_passwordContainsFullNameWithDotSeparator_throwsDomainException() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Bruce.Wayne", "Batman");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Bruce.Wayne", "Batman");
        assertInsecurePassword("Bruce-wayne@2026", "brucewayne@email.com", "Bruce.Wayne", "Batman");
    }

    @Test
    void validatePassword_passwordContainsFullNameWithDashSeparator_throwsDomainException() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Bruce-Wayne", "Batman");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Bruce-Wayne", "Batman");
        assertInsecurePassword("Bruce-wayne@2026", "brucewayne@email.com", "Bruce-Wayne", "Batman");
    }

    @Test
    void validatePassword_passwordContainsFullNameWithUnderscoreSeparator_throwsDomainException() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Bruce_Wayne", "Batman");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Bruce_Wayne", "Batman");
        assertInsecurePassword("Bruce_wayne@2026", "brucewayne@email.com", "Bruce_Wayne", "Batman");
    }

    @Test
    void validatePassword_passwordContainsGivenName_throwsDomainException() {

        assertInsecurePassword("Bruce@2026", "brucewayne@email.com", "Batman", "Bruce Wayne");
        assertInsecurePassword("Wayne@2026", "brucewayne@email.com", "Batman", "Bruce Wayne");
    }

    private void assertInsecurePassword(String pwd, String email, String fullName, String givenName) {

        RawPassword password = RawPassword.from(pwd);
        Email userEmail = Email.from(email);
        FullName userFullName = FullName.from(fullName);
        GivenName userGivenName = GivenName.from(givenName);
        UserProfileData userProfileData = UserProfileData.from(userFullName, userGivenName);

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> securityService.validatePassword(password, userEmail, userProfileData))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSECURE_PASSWORD);
    }
}
