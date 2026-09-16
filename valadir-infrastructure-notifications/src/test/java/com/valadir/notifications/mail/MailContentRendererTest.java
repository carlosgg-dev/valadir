package com.valadir.notifications.mail;

import com.valadir.domain.model.Language;
import com.valadir.notifications.config.MailRenderingTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MailContentRendererTest {

    private static final String OTP = "482913";
    private static final String EXPIRY = "15 minutes";
    private static final String LOCKOUT = "30 minutes";
    private static final Map<String, Object> MODEL = Map.of("otp", OTP, "expiry", EXPIRY, "lockout", LOCKOUT);

    private static final String UNRESOLVED_MESSAGE = "??";
    private static final String UNSUBSTITUTED_ARGUMENT = "{0}";

    // Its numbering system renders digits as Arabic-Indic, and its bundle does not exist here, so
    // it catches both a MessageSource falling back to the system locale and a locale-less format.
    private static final Locale LOCALE_WITH_DIFFERENT_DIGITS = Locale.forLanguageTag("ar-EG-u-nu-arab");

    private final MailContentRenderer renderer = MailRenderingTestFactory.contentRenderer();

    // The whole catalogue is swept here so that no adapter test has to: a new language is a constant
    // on the enum and brings no test of its own with it.
    @ParameterizedTest
    @MethodSource("everyTemplateInEveryLanguage")
    void render_everyTemplateAndLanguage_resolvesSubjectAndBothRepresentations(MailTemplate template, Language language) {

        var content = renderer.render(template, MODEL, language);

        assertThat(content.subject()).isNotBlank().doesNotContain(UNRESOLVED_MESSAGE);

        assertThat(content.html())
            .contains("lang=\"" + language.tag() + "\"")
            .doesNotContain(UNRESOLVED_MESSAGE, UNSUBSTITUTED_ARGUMENT);

        assertThat(content.plainText())
            .isNotBlank()
            .doesNotContain(UNRESOLVED_MESSAGE, UNSUBSTITUTED_ARGUMENT);
    }

    // A subject two languages share is one a bundle left untranslated and fell back to the base one.
    @ParameterizedTest
    @EnumSource(MailTemplate.class)
    void render_everyTemplate_writesADistinctSubjectPerLanguage(MailTemplate template) {

        var subjects = Arrays.stream(Language.values())
            .map(language -> renderer.render(template, MODEL, language).subject())
            .toList();

        assertThat(subjects).doesNotHaveDuplicates();
    }

    // The base language anchors the actual wording, which the sweep above only checks the shape of.
    @Test
    void render_englishLanguage_resolvesSubjectAndBothRepresentations() {

        var content = renderer.render(MailTemplate.ACCOUNT_ACTIVATION, MODEL, Language.EN);

        assertThat(content.subject()).isEqualTo("Valadir - account activation code");
        assertThat(content.html()).contains("Activate your account", OTP, EXPIRY);
        assertThat(content.plainText()).contains("Activate your account", OTP, EXPIRY);
    }

    // The two template modes are what keep the alternatives apart: rendering the text one as HTML
    // would deliver a plain part full of markup to the clients that cannot show the other.
    @Test
    void render_anyLanguage_writesTheTextRepresentationWithoutMarkup() {

        var content = renderer.render(MailTemplate.ACCOUNT_ACTIVATION, MODEL, Language.EN);

        assertThat(content.html()).contains("<table", "align=\"center\"");
        assertThat(content.plainText()).doesNotContain("<table", "<html", "style=");
    }

    @Test
    void render_exoticDefaultLocale_stillWritesInTheGivenLanguage() {

        var originalDefault = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_DIGITS);

            var content = renderer.render(MailTemplate.ACCOUNT_ACTIVATION, MODEL, Language.EN);

            assertThat(content.subject()).isEqualTo("Valadir - account activation code");
            assertThat(content.plainText()).contains("Activate your account", OTP);
        } finally {
            Locale.setDefault(originalDefault);
        }
    }

    private static Stream<Arguments> everyTemplateInEveryLanguage() {

        return Arrays.stream(MailTemplate.values())
            .flatMap(template -> Arrays.stream(Language.values()).map(language -> arguments(template, language)));
    }
}
