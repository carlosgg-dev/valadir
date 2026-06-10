package com.valadir.domain.service;

import com.valadir.common.error.ErrorCode;
import com.valadir.domain.exception.DomainException;
import com.valadir.domain.model.AccountId;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.RawPassword;
import com.valadir.domain.model.User;
import com.valadir.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PasswordSecurityServiceTest {

    private final PasswordSecurityService securityService = new PasswordSecurityService();

    @Test
    void validatePassword_securePassword_passes() {

        var password = RawPassword.from("Secure_P@ss_2026");
        var email = Email.from("bruce.wayne@email.com");
        var user = User.reconstitute(
            UserId.generate(),
            AccountId.generate(),
            FullName.from("Bruce Wayne"),
            GivenName.from("Batman")
        );

        assertThatCode(() -> securityService.validatePassword(password, email, user)).doesNotThrowAnyException();
    }

    @Test
    void validatePassword_nameTermsBelowMinLength_passes() {

        // Terms "jo", "li", "ann" are all < MIN_TERM_LENGTH (4) — they are ignored during validation
        var password = RawPassword.from("Xk9@Secure1");
        var email = Email.from("jo@example.com");
        var user = User.reconstitute(
            UserId.generate(),
            AccountId.generate(),
            FullName.from("Jo Li"),
            GivenName.from("Ann")
        );

        assertThatCode(() -> securityService.validatePassword(password, email, user)).doesNotThrowAnyException();
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

        var password = RawPassword.from(pwd);
        var userEmail = Email.from(email);
        var user = User.reconstitute(
            UserId.generate(),
            AccountId.generate(),
            FullName.from(fullName),
            GivenName.from(givenName)
        );

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> securityService.validatePassword(password, userEmail, user))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSECURE_PASSWORD);
    }
}
