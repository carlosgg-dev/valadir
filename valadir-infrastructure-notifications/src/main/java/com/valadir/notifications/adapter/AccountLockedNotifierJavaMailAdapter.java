package com.valadir.notifications.adapter;

import com.valadir.application.port.out.AccountLockedNotifier;
import com.valadir.domain.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;

public class AccountLockedNotifierJavaMailAdapter implements AccountLockedNotifier {

    private static final Logger log = LoggerFactory.getLogger(AccountLockedNotifierJavaMailAdapter.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public AccountLockedNotifierJavaMailAdapter(JavaMailSender mailSender, String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    @Override
    public void notifyAccountLocked(Email email, Duration lockoutDuration) {

        var message = getMailMessage(email, lockoutDuration);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Best-effort: a notification failure must never turn a login failure into a 500 nor add latency to the request.
            // Log and continue (unlike the activation adapter, where the failure is meaningful to the caller).
            log.warn("Failed to send account-locked notification to {}", email.value(), e);
        }
    }

    private SimpleMailMessage getMailMessage(Email email, Duration lockoutDuration) {

        var text = """
            We detected repeated failed sign-in attempts on your account, \
            so it has been temporarily locked for %d minutes. \
            If this was you, simply try again once the lock expires. \
            If it was not you, we recommend changing your password as a precaution.\
            """;

        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.value());
        message.setSubject("Valadir - suspicious sign-in activity");
        message.setText(text.formatted(lockoutDuration.toMinutes()));
        return message;
    }
}
