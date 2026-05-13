package com.valadir.notifications.config;

import com.valadir.application.port.out.AccountActivationPort;
import com.valadir.notifications.adapter.JavaMailAccountActivationAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class NotificationsWiring {

    @Bean
    AccountActivationPort accountActivationPort(JavaMailSender mailSender, @Value("${notifications.mail.from}") String fromAddress) {

        return new JavaMailAccountActivationAdapter(mailSender, fromAddress);
    }
}
