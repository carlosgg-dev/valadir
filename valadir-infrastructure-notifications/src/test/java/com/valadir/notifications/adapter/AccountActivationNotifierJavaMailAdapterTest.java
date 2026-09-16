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

    // Never the base language: an adapter that ignored the language it was handed and rendered in
    // English would pass every assertion below.
    private static final Language ACCOUNT_LANGUAGE = Language.ES;
    private static final String DOCUMENT_LANG = "lang=\"" + ACCOUNT_LANGUAGE.tag() + "\"";
    private static final String EXPIRY_WORDING = "15 minutos";

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
    void sendActivationCode_anyAccount_sendsBothAlternativesWithTheCodeAndItsExpiry() throws Exception {

        givenRealMimeMessages();

        adapter.sendActivationCode(notificationIn(ACCOUNT_LANGUAGE));

        then(mailSender).should().send(messageCaptor.capture());
        var message = messageCaptor.getValue();

        assertThat(message.getFrom()).extracting(Object::toString).containsExactly(FROM_ADDRESS);
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly(TO_ADDRESS);
        assertThat(message.getSubject()).isNotBlank();
        assertThat(MimeMessages.carriesBothAlternatives(message)).isTrue();

        // Both alternatives asserted apart: a reader whose client shows the plain part must not be
        // left without the code because only the HTML one was built.
        assertThat(MimeMessages.htmlOf(message)).contains(DOCUMENT_LANG, OTP.value(), EXPIRY_WORDING);
        assertThat(MimeMessages.plainTextOf(message)).contains(OTP.value(), EXPIRY_WORDING);
    }

    @Test
    void sendActivationCode_mailServerUnavailable_throwsInfrastructureException() {

        givenRealMimeMessages();
        willThrow(new MailSendException("SMTP down")).given(mailSender).send(any(MimeMessage.class));

        var notification = notificationIn(ACCOUNT_LANGUAGE);

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
