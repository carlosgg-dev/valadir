package com.valadir.notifications.adapter;

import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class PasswordResetNotifierJavaMailAdapter implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifierJavaMailAdapter.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PasswordResetNotifierJavaMailAdapter(JavaMailSender mailSender, String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendResetCode(Email email, PlainOtp plainOtp) {

        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.value());
        message.setSubject("Valadir - password reset code");
        message.setText("Your password reset code is: " + plainOtp.value() + "\n\nThis code expires in 15 minutes.");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send password reset code to {}", email.value(), e);
            throw new InfrastructureException("Mail server unavailable", e);
        }
    }
}
