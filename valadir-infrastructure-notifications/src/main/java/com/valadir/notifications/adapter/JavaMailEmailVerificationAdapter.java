package com.valadir.notifications.adapter;

import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.domain.model.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class JavaMailEmailVerificationAdapter implements EmailVerificationPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public JavaMailEmailVerificationAdapter(JavaMailSender mailSender, String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(Email email, String code) {

        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.value());
        message.setSubject("Valadir - register verification code");
        message.setText("Your verification code is: " + code + "\n\nThis code expires in 15 minutes.");
        mailSender.send(message);
    }
}
