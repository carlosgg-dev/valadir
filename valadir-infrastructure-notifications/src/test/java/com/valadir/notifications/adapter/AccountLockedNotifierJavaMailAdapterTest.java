package com.valadir.notifications.adapter;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.notifications.config.MailRenderingTestFactory;
import com.valadir.notifications.mail.MimeMailSender;
import com.valadir.notifications.support.MimeMessages;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccountLockedNotifierJavaMailAdapterTest {

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final Email TO_ADDRESS = Email.from("bruce.wayne@email.com");
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private AccountLockedNotifierJavaMailAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new AccountLockedNotifierJavaMailAdapter(
            MailRenderingTestFactory.contentRenderer(),
            new MimeMailSender(mailSender, FROM_ADDRESS),
            MailRenderingTestFactory.durationWording()
        );
    }

    @Test
    void notifyAccountLocked_englishAccount_sendsBothAlternativesWithTheLockoutWindow() throws Exception {

        givenRealMimeMessages();

        adapter.notifyAccountLocked(TO_ADDRESS, LOCKOUT_DURATION, Language.EN);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getFrom()).extracting(Object::toString).containsExactly(FROM_ADDRESS);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly(TO_ADDRESS.value());
        assertThat(message.getSubject()).isEqualTo("Valadir - suspicious sign-in activity");
        assertThat(MimeMessages.carriesBothAlternatives(message)).isTrue();
        assertThat(MimeMessages.htmlOf(message)).contains("30 minutes");
        assertThat(MimeMessages.plainTextOf(message)).contains("30 minutes");
    }

    @Test
    void notifyAccountLocked_spanishAccount_writesSubjectAndBodyInSpanish() throws Exception {

        givenRealMimeMessages();

        adapter.notifyAccountLocked(TO_ADDRESS, LOCKOUT_DURATION, Language.ES);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("Valadir - actividad de inicio de sesión sospechosa");
        assertThat(MimeMessages.plainTextOf(message)).contains("30 minutos");
    }

    @Test
    void notifyAccountLocked_mailServerUnavailable_isSwallowed() {

        givenRealMimeMessages();
        willThrow(new MailSendException("SMTP down")).given(mailSender).send(any(MimeMessage.class));

        assertThatNoException()
            .isThrownBy(() -> adapter.notifyAccountLocked(TO_ADDRESS, LOCKOUT_DURATION, Language.EN));
    }

    private void givenRealMimeMessages() {

        given(mailSender.createMimeMessage()).willAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());
    }
}
