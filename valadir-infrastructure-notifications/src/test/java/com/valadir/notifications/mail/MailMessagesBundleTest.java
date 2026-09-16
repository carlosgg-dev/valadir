package com.valadir.notifications.mail;

import com.valadir.domain.model.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the bundles rather than a class. A key added to the base bundle and forgotten in a
 * translation falls back to English silently, so every message still renders well formed and no
 * assertion on a rendered body would notice the mixed language.
 */
class MailMessagesBundleTest {

    private static final String BASE_BUNDLE = "mail/messages.properties";

    @ParameterizedTest
    @EnumSource(value = Language.class, mode = EnumSource.Mode.EXCLUDE, names = "EN")
    void bundle_everyTranslatedLanguage_carriesTheSameKeysAsTheBaseOne(Language language) {

        var base = bundleOf(BASE_BUNDLE);
        var translation = bundleOf("mail/messages_" + language.tag() + ".properties");

        assertThat(translation.stringPropertyNames())
            .containsExactlyInAnyOrderElementsOf(base.stringPropertyNames());
        assertThat(translation.stringPropertyNames())
            .allSatisfy(key -> assertThat(translation.getProperty(key)).isNotBlank());
    }

    private static Properties bundleOf(String resource) {

        try (InputStream stream = MailMessagesBundleTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new AssertionError("There is no bundle at " + resource);
            }

            var properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;

        } catch (IOException e) {
            throw new AssertionError("Could not read the bundle at " + resource, e);
        }
    }
}
