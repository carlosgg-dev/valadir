package com.valadir.notifications.adapter;

import com.valadir.domain.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JavaMailAccountActivationAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private JavaMailAccountActivationAdapter adapter;

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final String TO_ADDRESS = "user@example.com";
    private static final String CODE = "123456";

    @BeforeEach
    void setUp() {

        adapter = new JavaMailAccountActivationAdapter(mailSender, FROM_ADDRESS);
    }

    @Test
    void sendActivationCode_validInput_sendsMessageWithCorrectFields() {

        adapter.sendActivationCode(new Email(TO_ADDRESS), CODE);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(message.getTo()).containsExactly(TO_ADDRESS);
        assertThat(message.getSubject()).isEqualTo("Valadir - account activation code");
        assertThat(message.getText()).contains(CODE);
    }
}
