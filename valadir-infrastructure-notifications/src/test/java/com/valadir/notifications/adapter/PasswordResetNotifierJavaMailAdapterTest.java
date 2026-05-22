package com.valadir.notifications.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
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
class PasswordResetNotifierJavaMailAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private PasswordResetNotifierJavaMailAdapter adapter;

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final String TO_ADDRESS = "user@example.com";
    private static final PlainOtp PLAIN_OTP = PlainOtp.generate();

    @BeforeEach
    void setUp() {

        adapter = new PasswordResetNotifierJavaMailAdapter(mailSender, FROM_ADDRESS);
    }

    @Test
    void sendResetCode_validInput_sendsMessageWithCorrectFields() {

        adapter.sendResetCode(new Email(TO_ADDRESS), PLAIN_OTP);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(message.getTo()).containsExactly(TO_ADDRESS);
        assertThat(message.getSubject()).isEqualTo("Valadir - password reset code");
        assertThat(message.getText()).contains(PLAIN_OTP.value());
    }
}
