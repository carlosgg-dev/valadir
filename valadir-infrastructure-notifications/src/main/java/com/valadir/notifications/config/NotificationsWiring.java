package com.valadir.notifications.config;

import com.valadir.application.port.out.AccountActivationNotifier;
import com.valadir.application.port.out.PasswordResetNotifier;
import com.valadir.notifications.adapter.AccountActivationNotifierJavaMailAdapter;
import com.valadir.notifications.adapter.PasswordResetNotifierJavaMailAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class NotificationsWiring {

    @Bean
    AccountActivationNotifier accountActivationNotifier(
        JavaMailSender mailSender,
        @Value("${notifications.mail.from}") String fromAddress
    ) {

        return new AccountActivationNotifierJavaMailAdapter(mailSender, fromAddress);
    }

    @Bean
    PasswordResetNotifier passwordResetNotifier(
        JavaMailSender mailSender,
        @Value("${notifications.mail.from}") String fromAddress
    ) {

        return new PasswordResetNotifierJavaMailAdapter(mailSender, fromAddress);
    }
}
