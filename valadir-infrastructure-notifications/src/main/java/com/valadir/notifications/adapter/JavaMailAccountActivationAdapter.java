package com.valadir.notifications.adapter;

import com.valadir.application.port.out.AccountActivationPort;
import com.valadir.domain.model.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class JavaMailAccountActivationAdapter implements AccountActivationPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public JavaMailAccountActivationAdapter(JavaMailSender mailSender, String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendActivationCode(Email email, String code) {

        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.value());
        message.setSubject("Valadir - account activation code");
        message.setText("Your account activation code is: " + code + "\n\nThis code expires in 15 minutes.");
        mailSender.send(message);
    }
}
