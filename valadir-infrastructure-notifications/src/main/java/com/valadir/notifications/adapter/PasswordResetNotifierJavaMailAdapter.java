package com.valadir.notifications.adapter;

import com.valadir.application.port.out.OtpNotification;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.notifications.mail.DurationWording;
import com.valadir.notifications.mail.MailContentRenderer;
import com.valadir.notifications.mail.MailTemplate;
import com.valadir.notifications.mail.MimeMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;

import java.util.Map;

public class PasswordResetNotifierJavaMailAdapter implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifierJavaMailAdapter.class);

    private final MailContentRenderer contentRenderer;
    private final MimeMailSender mailSender;
    private final DurationWording durationWording;

    public PasswordResetNotifierJavaMailAdapter(
        MailContentRenderer contentRenderer,
        MimeMailSender mailSender,
        DurationWording durationWording
    ) {

        this.contentRenderer = contentRenderer;
        this.mailSender = mailSender;
        this.durationWording = durationWording;
    }

    @Override
    public void sendResetCode(OtpNotification notification) {

        var locale = notification.language().toLocale();
        var model = Map.<String, Object>of(
            "otp", notification.otp().value(),
            "expiry", durationWording.wordingOf(notification.ttl(), locale)
        );

        var content = contentRenderer.render(MailTemplate.PASSWORD_RESET, model, notification.language());

        try {
            mailSender.send(notification.email(), content);
        } catch (MailException e) {
            log.error("Failed to send password reset code to {}", notification.email().value(), e);
            throw new InfrastructureException("Mail server unavailable", e);
        }
    }
}
