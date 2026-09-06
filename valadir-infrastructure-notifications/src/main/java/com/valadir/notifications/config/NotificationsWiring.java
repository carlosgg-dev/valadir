package com.valadir.notifications.config;

import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.notifications.adapter.AccountActivationNotifierJavaMailAdapter;
import com.valadir.notifications.adapter.AccountLockedNotifierJavaMailAdapter;
import com.valadir.notifications.adapter.PasswordResetNotifierJavaMailAdapter;
import com.valadir.notifications.mail.DurationWording;
import com.valadir.notifications.mail.MailContentRenderer;
import com.valadir.notifications.mail.MimeMailSender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(NotificationsProperties.class)
class NotificationsWiring {

    private static final String TEMPLATE_PREFIX = "templates/mail/";

    @Bean
    MessageSource mailMessageSource() {

        var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("mail/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // Without this, a language with no bundle falls back to the JVM default — the language of
        // wherever the service runs. False falls back to the base bundle instead, which is English.
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    /**
     * The Spring engine, not the plain one: since Thymeleaf 3.1 the standard dialect's OGNL
     * evaluator is an optional dependency, while this one evaluates with SpEL, already on the
     * classpath. It also routes {@code #{...}} through our own bundle rather than a template-local one.
     */
    @Bean
    ITemplateEngine mailTemplateEngine(MessageSource mailMessageSource) {

        var engine = new SpringTemplateEngine();
        engine.addTemplateResolver(templateResolver(TemplateMode.HTML, "*.html"));
        engine.addTemplateResolver(templateResolver(TemplateMode.TEXT, "*.txt"));
        engine.setTemplateEngineMessageSource(mailMessageSource);
        return engine;
    }

    @Bean
    MailContentRenderer mailContentRenderer(ITemplateEngine mailTemplateEngine, MessageSource mailMessageSource) {

        return new MailContentRenderer(mailTemplateEngine, mailMessageSource);
    }

    @Bean
    DurationWording durationWording(MessageSource mailMessageSource) {

        return new DurationWording(mailMessageSource);
    }

    @Bean
    MimeMailSender mimeMailSender(JavaMailSender mailSender, NotificationsProperties properties) {

        return new MimeMailSender(mailSender, properties.from());
    }

    @Bean
    AccountActivationNotifier accountActivationNotifier(
        MailContentRenderer contentRenderer,
        MimeMailSender mailSender,
        DurationWording durationWording
    ) {

        return new AccountActivationNotifierJavaMailAdapter(contentRenderer, mailSender, durationWording);
    }

    @Bean
    AccountLockedNotifier accountLockedNotifier(
        MailContentRenderer contentRenderer,
        MimeMailSender mailSender,
        DurationWording durationWording
    ) {

        return new AccountLockedNotifierJavaMailAdapter(contentRenderer, mailSender, durationWording);
    }

    @Bean
    PasswordResetNotifier passwordResetNotifier(
        MailContentRenderer contentRenderer,
        MimeMailSender mailSender,
        DurationWording durationWording
    ) {

        return new PasswordResetNotifierJavaMailAdapter(contentRenderer, mailSender, durationWording);
    }

    private static ClassLoaderTemplateResolver templateResolver(TemplateMode mode, String pattern) {

        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(TEMPLATE_PREFIX);
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setResolvablePatterns(Set.of(pattern));
        return resolver;
    }
}
