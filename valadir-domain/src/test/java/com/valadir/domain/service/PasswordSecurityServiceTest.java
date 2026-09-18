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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PasswordSecurityServiceTest {

    private static final String EMAIL = "brucewayne@email.com";

    // The email cases below pair it with a name that shares nothing with the address: with an
    // overlapping name the name rule answers first and the case proves nothing about the address.
    private static final String UNRELATED_FULL_NAME = "Clark Kent";

    // Case folding is locale dependent: in this one an uppercase I folds to a dotless "ı" instead
    // of "i", so a term lowercased with it no longer matches its Locale.ROOT form.
    private static final Locale LOCALE_WITH_DIFFERENT_CASE_FOLDING = Locale.forLanguageTag("tr");

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

    // ── Email (checked per word of the local part, plus the whole address) ──

    @Test
    void validatePassword_passwordContainsLocalPart_throwsDomainException() {

        assertInsecurePassword("brucewayne!A1", EMAIL, UNRELATED_FULL_NAME, "Batman");
    }

    // `+` belongs here and not only among the name separators: plus-addressing is what a real address
    // carries, and without it the tag welds the identity into one term nothing matches
    @ParameterizedTest
    @ValueSource(strings = {".", "-", "_", "+"})
    void validatePassword_localPartWordsSeparatedBySymbol_throwsDomainException(String separator) {

        String email = "bruce" + separator + "wayne@email.com";
        assertInsecurePassword("Bruce@2026", email, UNRELATED_FULL_NAME, "Batman");
        assertInsecurePassword("Wayne@2026", email, UNRELATED_FULL_NAME, "Batman");
    }

    @Test
    void validatePassword_localPartBelowMinLength_passes() {

        // "ana" has 3 chars (< MIN_TERM_LENGTH 4): the address contributes no term, exactly as a short name does not
        assertSecurePassword("Ana@Secure9!", "ana@email.com", UNRELATED_FULL_NAME, "Batman");
    }

    @Test
    void validatePassword_passwordContainsWholeAddressOfAShortLocalPart_throwsDomainException() {

        // The one shape only the whole-address check can answer: "ana" is below the threshold, so no
        // term is left to match and the name shares nothing with the address
        assertInsecurePassword("ana@email.com1A", "ana@email.com", UNRELATED_FULL_NAME, "Batman");
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

    // Both sides of the check are lowercased, so a locale only shows up when the uppercase letter
    // sits on one side alone: each row puts it on a different side, covering both conversions.
    // With the personal data folded by the default locale the term stops matching and the password
    // is accepted.
    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({
        "Isabel Wayne, isabel@2026A",
        "Bill Wayne, BILL@2026a"
    })
    void validatePassword_personalDataFoldedByTheDefaultLocale_throwsDomainException(String fullName, String password) {

        Locale defaultLocale = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_CASE_FOLDING);

            assertInsecurePassword(password, EMAIL, fullName, "Batman");

        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    // U+00F1 is the ñ as one character, n + U+0303 an n carrying a combining tilde: one letter on screen, two
    // sequences to a comparison. The password arrives composed by RawPassword and the name by nothing, so this is the
    // side the service has to compose itself. Only one row: with the name composed instead, RawPassword alone would
    // make the two meet and the case would pass without the service doing anything.
    // Written as escapes, so a tool that normalized this file cannot turn the pair into one spelling.
    @Test
    void validatePassword_personalDataSpelledInTheOtherNormalForm_throwsDomainException() {

        assertInsecurePassword("Pe\u00F1a@2026", EMAIL, "Pen\u0303a Wayne", "Batman");
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
