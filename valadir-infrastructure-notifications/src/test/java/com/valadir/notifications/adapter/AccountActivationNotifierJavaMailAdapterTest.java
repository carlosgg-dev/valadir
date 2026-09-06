package com.valadir.notifications.adapter;

import com.valadir.application.port.out.OtpNotification;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.PlainOtp;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AccountActivationNotifierJavaMailAdapterTest {

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final String TO_ADDRESS = "bruce.wayne@email.com";
    private static final PlainOtp OTP = PlainOtp.from("482913");
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private AccountActivationNotifierJavaMailAdapter adapter;

    @BeforeEach
    void setUp() {

        adapter = new AccountActivationNotifierJavaMailAdapter(
            MailRenderingTestFactory.contentRenderer(),
            new MimeMailSender(mailSender, FROM_ADDRESS),
            MailRenderingTestFactory.durationWording()
        );
    }

    @Test
    void sendActivationCode_englishAccount_sendsBothAlternativesWithTheCodeAndItsExpiry() throws Exception {

        givenRealMimeMessages();

        adapter.sendActivationCode(notificationIn(Language.EN));

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getFrom()).extracting(Object::toString).containsExactly(FROM_ADDRESS);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly(TO_ADDRESS);
        assertThat(message.getSubject()).isEqualTo("Valadir - account activation code");
        assertThat(MimeMessages.carriesBothAlternatives(message)).isTrue();

        // Both alternatives asserted apart: a reader whose client shows the plain part must not be
        // left without the code because only the HTML one was built.
        assertThat(MimeMessages.htmlOf(message)).contains(OTP.value(), "15 minutes");
        assertThat(MimeMessages.plainTextOf(message)).contains(OTP.value(), "15 minutes");
    }

    @Test
    void sendActivationCode_spanishAccount_writesSubjectAndBodyInSpanish() throws Exception {

        givenRealMimeMessages();

        adapter.sendActivationCode(notificationIn(Language.ES));

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("Valadir - código de activación de cuenta");
        assertThat(MimeMessages.htmlOf(message)).contains("Activa tu cuenta", "15 minutos");
        assertThat(MimeMessages.plainTextOf(message)).contains(OTP.value(), "15 minutos");
    }

    @Test
    void sendActivationCode_mailServerUnavailable_throwsInfrastructureException() {

        givenRealMimeMessages();
        willThrow(new MailSendException("SMTP down")).given(mailSender).send(any(MimeMessage.class));

        var notification = notificationIn(Language.EN);

        assertThatExceptionOfType(InfrastructureException.class)
            .isThrownBy(() -> adapter.sendActivationCode(notification));
    }

    private void givenRealMimeMessages() {

        given(mailSender.createMimeMessage()).willAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());
    }

    private static OtpNotification notificationIn(Language language) {

        return new OtpNotification(Email.from(TO_ADDRESS), OTP, OTP_TTL, language);
    }
}
