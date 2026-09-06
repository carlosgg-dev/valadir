package com.valadir.notifications.mail;

import com.valadir.domain.model.Email;
import jakarta.mail.MessagingException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

/**
 * The only place that talks to JavaMail. Composing with both representations is what makes the body a
 * {@code multipart/alternative}: a client that renders HTML shows the formatted version, one that
 * does not still shows a readable mail instead of markup.
 */
public class MimeMailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MimeMailSender(JavaMailSender mailSender, String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(Email recipient, MailContent content) {

        var message = mailSender.createMimeMessage();

        try {
            // The explicit charset is what carries an accented subject or body intact; without it
            // JavaMail falls back to the platform encoding, which the deployment decides.
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipient.value());
            helper.setSubject(content.subject());
            helper.setText(content.plainText(), content.html());
        } catch (MessagingException e) {
            // Wrapped as a MailException so a composition failure reaches each adapter's own
            // policy through the same catch as a delivery failure.
            throw new MailPreparationException("Could not compose the message", e);
        }

        mailSender.send(message);
    }
}
