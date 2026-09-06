package com.valadir.notifications.mail;

import com.valadir.domain.model.Language;
import org.springframework.context.MessageSource;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

public class MailContentRenderer {

    private final ITemplateEngine templateEngine;
    private final MessageSource messageSource;

    public MailContentRenderer(ITemplateEngine templateEngine, MessageSource messageSource) {

        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
    }

    public MailContent render(MailTemplate template, Map<String, Object> model, Language language) {

        var locale = language.toLocale();
        var context = new Context(locale, model);

        return new MailContent(
            messageSource.getMessage(template.subjectKey(), null, locale),
            templateEngine.process(template.textTemplate(), context),
            templateEngine.process(template.htmlTemplate(), context)
        );
    }
}
