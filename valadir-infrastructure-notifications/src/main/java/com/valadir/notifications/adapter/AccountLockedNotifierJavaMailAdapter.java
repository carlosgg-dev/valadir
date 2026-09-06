package com.valadir.notifications.adapter;

import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.notifications.mail.DurationWording;
import com.valadir.notifications.mail.MailContentRenderer;
import com.valadir.notifications.mail.MailTemplate;
import com.valadir.notifications.mail.MimeMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.util.Map;

public class AccountLockedNotifierJavaMailAdapter implements AccountLockedNotifier {

    private static final Logger log = LoggerFactory.getLogger(AccountLockedNotifierJavaMailAdapter.class);

    private final MailContentRenderer contentRenderer;
    private final MimeMailSender mailSender;
    private final DurationWording durationWording;

    public AccountLockedNotifierJavaMailAdapter(
        MailContentRenderer contentRenderer,
        MimeMailSender mailSender,
        DurationWording durationWording
    ) {

        this.contentRenderer = contentRenderer;
        this.mailSender = mailSender;
        this.durationWording = durationWording;
    }

    @Async
    @Override
    public void notifyAccountLocked(Email email, Duration lockoutDuration, Language language) {

        var model = Map.<String, Object>of(
            "lockout", durationWording.wordingOf(lockoutDuration, language.toLocale())
        );

        var content = contentRenderer.render(MailTemplate.ACCOUNT_LOCKED, model, language);

        try {
            mailSender.send(email, content);
        } catch (MailException e) {
            // Best-effort: a notification failure must never turn a login failure into a 500 nor add latency to the request.
            // Log and continue (unlike the activation adapter, where the failure is meaningful to the caller).
            log.warn("Failed to send account-locked notification to {}", email.value(), e);
        }
    }
}
