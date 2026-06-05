package com.valadir.notifications.adapter;

import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccountActivationNotifierJavaMailAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private AccountActivationNotifierJavaMailAdapter adapter;

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final String TO_ADDRESS = "user@example.com";
    private static final PlainOtp PLAIN_OTP = PlainOtp.generate();

    @BeforeEach
    void setUp() {

        adapter = new AccountActivationNotifierJavaMailAdapter(mailSender, FROM_ADDRESS);
    }

    @Test
    void sendActivationCode_validInput_sendsMessageWithCorrectFields() {

        adapter.sendActivationCode(Email.from(TO_ADDRESS), PLAIN_OTP);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(message.getTo()).containsExactly(TO_ADDRESS);
        assertThat(message.getSubject()).isEqualTo("Valadir - account activation code");
        assertThat(message.getText()).contains(PLAIN_OTP.value());
    }

    @Test
    void sendActivationCode_mailServerUnavailable_throwsInfrastructureException() {

        var email = Email.from(TO_ADDRESS);

        willThrow(MailSendException.class).given(mailSender).send(any(SimpleMailMessage.class));

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.sendActivationCode(email, PLAIN_OTP))
            .withCauseInstanceOf(MailException.class);
    }
}
