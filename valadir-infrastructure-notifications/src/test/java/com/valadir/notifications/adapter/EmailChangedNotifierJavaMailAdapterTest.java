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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class EmailChangedNotifierJavaMailAdapterTest {

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final Email PREVIOUS_ADDRESS = Email.from("bruce.wayne@email.com");

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private EmailChangedNotifierJavaMailAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new EmailChangedNotifierJavaMailAdapter(
            MailRenderingTestFactory.contentRenderer(),
            new MimeMailSender(mailSender, FROM_ADDRESS)
        );
    }

    @Test
    void notifyEmailChanged_englishAccount_sendsBothAlternativesToThePreviousAddress() throws Exception {

        given(mailSender.createMimeMessage()).willAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());

        adapter.notifyEmailChanged(PREVIOUS_ADDRESS, Language.EN);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getFrom()).extracting(Object::toString).containsExactly(FROM_ADDRESS);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly(PREVIOUS_ADDRESS.value());
        assertThat(message.getSubject()).isEqualTo("Valadir - your email was changed");
        assertThat(MimeMessages.carriesBothAlternatives(message)).isTrue();
        assertThat(MimeMessages.htmlOf(message)).contains("no longer signs in");
        assertThat(MimeMessages.plainTextOf(message)).contains("no longer signs in");
    }

    @Test
    void notifyEmailChanged_spanishAccount_writesSubjectAndBodyInSpanish() throws Exception {

        given(mailSender.createMimeMessage()).willAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());

        adapter.notifyEmailChanged(PREVIOUS_ADDRESS, Language.ES);

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("Valadir - tu correo electrónico ha cambiado");
        assertThat(MimeMessages.plainTextOf(message)).contains("ya no sirve para iniciar sesión");
    }

    @Test
    void notifyEmailChanged_mailServerUnavailable_isSwallowed() {

        given(mailSender.createMimeMessage()).willAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());
        willThrow(new MailSendException("SMTP down")).given(mailSender).send(any(MimeMessage.class));

        assertThatNoException()
            .isThrownBy(() -> adapter.notifyEmailChanged(PREVIOUS_ADDRESS, Language.EN));
    }
}
