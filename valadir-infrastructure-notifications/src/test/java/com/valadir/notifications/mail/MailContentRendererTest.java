package com.valadir.notifications.mail;

import com.valadir.domain.model.Language;
import com.valadir.notifications.config.MailRenderingTestFactory;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MailContentRendererTest {

    private static final String OTP = "482913";
    private static final String EXPIRY = "15 minutes";
    private static final Map<String, Object> MODEL = Map.of("otp", OTP, "expiry", EXPIRY);

    // Its numbering system renders digits as Arabic-Indic, and its bundle does not exist here, so
    // it catches both a MessageSource falling back to the system locale and a locale-less format.
    private static final Locale LOCALE_WITH_DIFFERENT_DIGITS = Locale.forLanguageTag("ar-EG-u-nu-arab");

    private final MailContentRenderer renderer = MailRenderingTestFactory.contentRenderer();

    @Test
    void render_englishLanguage_resolvesSubjectAndBothRepresentations() {

        var content = renderer.render(MailTemplate.ACCOUNT_ACTIVATION, MODEL, Language.EN);

        assertThat(content.subject()).isEqualTo("Valadir - account activation code");
        assertThat(content.html()).contains("Activate your account", OTP, EXPIRY);
        assertThat(content.plainText()).contains("Activate your account", OTP, EXPIRY);
    }

    @Test
    void render_spanishLanguage_resolvesSubjectAndBothRepresentations() {

        var content = renderer.render(MailTemplate.ACCOUNT_ACTIVATION, MODEL, Language.ES);

        assertThat(content.subject()).isEqualTo("Valadir - código de activación de cuenta");
        assertThat(content.html()).contains("Activa tu cuenta", OTP);
        assertThat(content.plainText()).contains("Activa tu cuenta", OTP);
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
}
