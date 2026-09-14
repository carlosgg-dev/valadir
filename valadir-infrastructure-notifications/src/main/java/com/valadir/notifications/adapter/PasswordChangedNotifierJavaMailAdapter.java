package com.valadir.notifications.adapter;

import com.valadir.application.port.out.PasswordChangedNotifier;
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

public class PasswordChangedNotifierJavaMailAdapter implements PasswordChangedNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangedNotifierJavaMailAdapter.class);

    private final MailContentRenderer contentRenderer;
    private final MimeMailSender mailSender;

    public PasswordChangedNotifierJavaMailAdapter(MailContentRenderer contentRenderer, MimeMailSender mailSender) {

        this.contentRenderer = contentRenderer;
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void notifyPasswordChanged(Email email, Language language) {

        var content = contentRenderer.render(MailTemplate.PASSWORD_CHANGED, Map.of(), language);

        try {
            mailSender.send(email, content);
        } catch (MailException e) {
            // Best-effort: the change is already applied, so a lost alert must neither fail it nor add latency to it
            log.warn("Failed to send password-changed notification to {}", email.value(), e);
        }
    }
}
