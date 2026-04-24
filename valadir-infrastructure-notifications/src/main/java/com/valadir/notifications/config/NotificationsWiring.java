package com.valadir.notifications.config;

import com.valadir.application.port.out.EmailVerificationPort;
import com.valadir.notifications.adapter.JavaMailEmailVerificationAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class NotificationsWiring {

    @Bean
    EmailVerificationPort emailVerificationPort(JavaMailSender mailSender, @Value("${notifications.mail.from}") String fromAddress) {

        return new JavaMailEmailVerificationAdapter(mailSender, fromAddress);
    }
}
