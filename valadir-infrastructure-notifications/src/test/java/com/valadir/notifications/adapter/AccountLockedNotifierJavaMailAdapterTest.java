package com.valadir.notifications.adapter;

import com.valadir.domain.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccountLockedNotifierJavaMailAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private AccountLockedNotifierJavaMailAdapter adapter;

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final String TO_ADDRESS = "bruce.wayne@email.com";
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);

    @BeforeEach
    void setUp() {

        adapter = new AccountLockedNotifierJavaMailAdapter(mailSender, FROM_ADDRESS);
    }

    @Test
    void notifyAccountLocked_validInput_sendsMessageWithCorrectFields() {

        adapter.notifyAccountLocked(Email.from(TO_ADDRESS), LOCKOUT_DURATION);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(message.getTo()).containsExactly(TO_ADDRESS);
        assertThat(message.getSubject()).isEqualTo("Valadir - suspicious sign-in activity");
        assertThat(message.getText()).contains(String.valueOf(LOCKOUT_DURATION.toMinutes()));
    }

    @Test
    void notifyAccountLocked_mailServerUnavailable_doesNotPropagate() {

        willThrow(MailSendException.class).given(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException()
            .isThrownBy(() -> adapter.notifyAccountLocked(Email.from(TO_ADDRESS), LOCKOUT_DURATION));
    }
}
