package com.valadir.notifications.adapter;

import com.valadir.application.port.out.EmailChangeNotifier;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.notifications.mail.DurationWording;
import com.valadir.notifications.mail.MailContentRenderer;
import com.valadir.notifications.mail.MailTemplate;
import com.valadir.notifications.mail.MimeMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;

import java.util.Map;

public class EmailChangeNotifierJavaMailAdapter implements EmailChangeNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailChangeNotifierJavaMailAdapter.class);

    private final MailContentRenderer contentRenderer;
    private final MimeMailSender mailSender;
    private final DurationWording durationWording;

    public EmailChangeNotifierJavaMailAdapter(
        MailContentRenderer contentRenderer,
        MimeMailSender mailSender,
        DurationWording durationWording
    ) {

        this.contentRenderer = contentRenderer;
        this.mailSender = mailSender;
        this.durationWording = durationWording;
    }

    @Override
    public void sendConfirmationCode(OtpNotification notification) {

        var locale = notification.language().toLocale();
        var model = Map.<String, Object>of(
            "otp", notification.otp().value(),
            "expiry", durationWording.wordingOf(notification.ttl(), locale)
        );

        var content = contentRenderer.render(MailTemplate.EMAIL_CHANGE, model, notification.language());

        try {
            mailSender.send(notification.email(), content);
        } catch (MailException e) {
            log.error("Failed to send email change code to {}", notification.email().value(), e);
            throw new InfrastructureException("Mail server unavailable", e);
        }
    }
}
