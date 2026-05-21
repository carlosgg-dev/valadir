package com.valadir.notifications.adapter;

import com.valadir.application.otp.PlainOtp;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.domain.model.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class PasswordResetNotifierJavaMailAdapter implements PasswordResetNotifier {

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
        mailSender.send(message);
    }
}
