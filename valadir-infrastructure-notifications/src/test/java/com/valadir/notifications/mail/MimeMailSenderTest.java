package com.valadir.notifications.mail;

import com.valadir.domain.model.Email;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Only the composition failure: the six notifier adapter tests already compose and assert real
 * messages through this class, so a happy path here would be the same proof written twice.
 */
@ExtendWith(MockitoExtension.class)
class MimeMailSenderTest {

    private static final String FROM_ADDRESS = "noreply@valadir.com";
    private static final Email RECIPIENT = Email.from("bruce.wayne@email.com");
    private static final MailContent CONTENT = new MailContent("Subject", "Plain text", "<p>Html</p>");

    @Mock
    private JavaMailSender mailSender;

    @Test
    void send_compositionFails_wrapsTheCauseAndSendsNothing() {

        var mimeMailSender = new MimeMailSender(mailSender, FROM_ADDRESS);

        given(mailSender.createMimeMessage()).willReturn(messageRefusingToBeComposed());

        assertThatExceptionOfType(MailPreparationException.class)
            .isThrownBy(() -> mimeMailSender.send(RECIPIENT, CONTENT))
            .withCauseInstanceOf(MessagingException.class);

        then(mailSender).should(never()).send(any(MimeMessage.class));
    }

    /**
     * @return a message that refuses the first header the helper writes, which is what makes
     * JavaMail raise a checked {@link MessagingException} out of the composition block.
     */
    private static MimeMessage messageRefusingToBeComposed() {

        return new MimeMessage((Session) null) {

            @Override
            public void setFrom(Address address) throws MessagingException {

                throw new MessagingException("Refused");
            }
        };
    }
}
