package com.valadir.notifications.adapter;

import com.valadir.application.otp.PlainOtp;
import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.domain.model.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class AccountActivationNotifierJavaMailAdapter implements AccountActivationNotifier {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public AccountActivationNotifierJavaMailAdapter(JavaMailSender mailSender, String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendActivationCode(Email email, PlainOtp plainOtp) {

        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email.value());
        message.setSubject("Valadir - account activation code");
        message.setText("Your account activation code is: " + plainOtp.value() + "\n\nThis code expires in 15 minutes.");
        mailSender.send(message);
    }
}
