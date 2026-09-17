package com.valadir.notifications.adapter;

import com.valadir.application.port.out.EmailChangedNotifier;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.notifications.mail.MailContentRenderer;
import com.valadir.notifications.mail.MailTemplate;
import com.valadir.notifications.mail.MimeMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public class EmailChangedNotifierJavaMailAdapter implements EmailChangedNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailChangedNotifierJavaMailAdapter.class);

    private final MailContentRenderer contentRenderer;
    private final MimeMailSender mailSender;

    public EmailChangedNotifierJavaMailAdapter(MailContentRenderer contentRenderer, MimeMailSender mailSender) {

        this.contentRenderer = contentRenderer;
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void notifyEmailChanged(Email email, Language language) {

        var content = contentRenderer.render(MailTemplate.EMAIL_CHANGED, Map.of(), language);

        try {
            mailSender.send(email, content);
        } catch (MailException e) {
            // Best-effort: the change is already applied, so a lost alert must neither fail it nor add latency to it
            log.warn("Failed to send email-changed notification to the previous address", e);
        }
    }
}
