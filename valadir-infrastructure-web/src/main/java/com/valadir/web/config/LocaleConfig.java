package com.valadir.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

@Configuration
public class LocaleConfig {

    /**
     * Spring's default resolver answers a request without {@code Accept-Language} with the JVM
     * default, so the language a reader is written to would be decided by wherever the service
     * happens to run. An explicit default makes "no preference expressed" mean English everywhere.
     *
     * <p>No supported list is declared on purpose: whichever locale the client asks for is passed
     * on, and deciding which of them we actually write is the domain's job.
     */
    @Bean
    LocaleResolver localeResolver() {

        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}
