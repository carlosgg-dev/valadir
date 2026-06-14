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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PasswordSecurityServiceTest {

    private static final String EMAIL = "brucewayne@email.com";

    private final PasswordSecurityService securityService = new PasswordSecurityService();

    // ── Happy path ──

    @Test
    void validatePassword_passwordWithNoPersonalData_passes() {

        assertSecurePassword("Secure_P@ss_2026", "bruce.wayne@email.com", "Bruce Wayne", "Batman");
    }

    // ── Per-word length threshold (isolated with single-word names) ──

    @Test
    void validatePassword_termBelowMinLength_passes() {

        // "ann" has 3 chars (< MIN_TERM_LENGTH 4): it is ignored even when the password contains it
        assertSecurePassword("Ann@Secure9!", EMAIL, "Ann", "Batman");
    }

    @Test
    void validatePassword_termAtMinLength_throwsDomainException() {

        // "jack" has exactly MIN_TERM_LENGTH (4) chars: it is checked and must reject the password
        assertInsecurePassword("Jack@2026", EMAIL, "Jack", "Batman");
    }

    // ── Email ──

    @Test
    void validatePassword_passwordContainsEmail_throwsDomainException() {

        assertInsecurePassword("bruce.wayne@email.com1A", "bruce.wayne@email.com", "Bruce Wayne", "Batman");
    }

    // ── Full name (checked per word) ──

    @Test
    void validatePassword_passwordContainsFirstFullNameWord_throwsDomainException() {

        assertInsecurePassword("Bruce@2026", EMAIL, "Bruce Wayne", "Batman");
    }

    @Test
    void validatePassword_passwordContainsSecondFullNameWord_throwsDomainException() {

        assertInsecurePassword("Wayne@2026", EMAIL, "Bruce Wayne", "Batman");
    }

    @ParameterizedTest
    @ValueSource(strings = {".", "-", "_"})
    void validatePassword_fullNameWordsSeparatedBySymbol_throwsDomainException(String separator) {

        String fullName = "Bruce" + separator + "Wayne";
        assertInsecurePassword("Bruce@2026", EMAIL, fullName, "Batman");
        assertInsecurePassword("Wayne@2026", EMAIL, fullName, "Batman");
    }

    @Test
    void validatePassword_multiWordNameShortWordPresent_passes() {

        // In "Jo Wayne", the short word "jo" (2 < MIN_TERM_LENGTH) is ignored even when present;
        // the long word "wayne" is absent, so the password is accepted (per-word, not per-name)
        assertSecurePassword("Jo@Secure9!", EMAIL, "Jo Wayne", "Batman");
    }

    // ── Given name (optional, checked per word) ──

    @Test
    void validatePassword_passwordContainsGivenNameWord_throwsDomainException() {

        assertInsecurePassword("Bruce@2026", EMAIL, "Batman", "Bruce Wayne");
        assertInsecurePassword("Wayne@2026", EMAIL, "Batman", "Bruce Wayne");
    }

    @ParameterizedTest
    @ValueSource(strings = {".", "-", "_"})
    void validatePassword_givenNameWordsSeparatedBySymbol_throwsDomainException(String separator) {

        String givenName = "Bruce" + separator + "Wayne";
        assertInsecurePassword("Bruce@2026", EMAIL, "Batman", givenName);
        assertInsecurePassword("Wayne@2026", EMAIL, "Batman", givenName);
    }

    @Test
    void validatePassword_userWithoutGivenName_passes() {

        // Given name is optional (blank -> null): a clean password is accepted, the absent term is skipped
        assertSecurePassword("Secure_P@ss_2026", "bruce.wayne@email.com", "Bruce Wayne", "   ");
    }

    @Test
    void validatePassword_userWithoutGivenNamePasswordContainsFullName_throwsDomainException() {

        // The full name is still enforced when the given name is absent (null term skipped, not fatal)
        assertInsecurePassword("Wayne@2026", EMAIL, "Bruce Wayne", "   ");
    }

    private void assertSecurePassword(String pwd, String email, String fullName, String givenName) {

        assertThatCode(() -> validate(pwd, email, fullName, givenName)).doesNotThrowAnyException();
    }

    private void assertInsecurePassword(String pwd, String email, String fullName, String givenName) {

        assertThatExceptionOfType(DomainException.class)
            .isThrownBy(() -> validate(pwd, email, fullName, givenName))
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSECURE_PASSWORD);
    }

    private void validate(String pwd, String email, String fullName, String givenName) {

        var user = User.reconstitute(
            UserId.generate(),
            AccountId.generate(),
            FullName.from(fullName),
            GivenName.from(givenName)
        );

        securityService.validatePassword(RawPassword.from(pwd), Email.from(email), user);
    }
}
