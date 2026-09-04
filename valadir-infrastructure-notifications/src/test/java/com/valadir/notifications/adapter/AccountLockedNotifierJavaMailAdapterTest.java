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
import java.util.Locale;

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
    private static final Duration SUB_MINUTE_LOCKOUT = Duration.ofSeconds(45);
    private static final Duration SINGLE_MINUTE_LOCKOUT = Duration.ofMinutes(1);

    // Its numbering system renders digits as Arabic-Indic, so a %d formatted with the default
    // locale reaches the owner as a number they cannot compare with the one they were told.
    private static final Locale LOCALE_WITH_DIFFERENT_DIGITS = Locale.forLanguageTag("ar-EG-u-nu-arab");

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
    void notifyAccountLocked_subMinuteLockout_reportsItInSeconds() {

        adapter.notifyAccountLocked(Email.from(TO_ADDRESS), SUB_MINUTE_LOCKOUT);
        
        then(mailSender).should().send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getText()).contains("45 seconds");
    }

    @Test
    void notifyAccountLocked_oneMinuteLockout_readsInSingular() {

        adapter.notifyAccountLocked(Email.from(TO_ADDRESS), SINGLE_MINUTE_LOCKOUT);

        then(mailSender).should().send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getText()).contains("1 minute.");
    }

    @Test
    void notifyAccountLocked_defaultLocaleWithOtherDigits_reportsTheLockoutInWesternDigits() {

        Locale defaultLocale = Locale.getDefault();

        try {
            Locale.setDefault(LOCALE_WITH_DIFFERENT_DIGITS);

            adapter.notifyAccountLocked(Email.from(TO_ADDRESS), LOCKOUT_DURATION);

        } finally {
            Locale.setDefault(defaultLocale);
        }

        // The wording is ours, not the reader's: where the JVM runs must not decide which digits
        // the owner is asked to wait out.
        then(mailSender).should().send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getText()).contains("30 minutes");
    }

    @Test
    void notifyAccountLocked_mailServerUnavailable_doesNotPropagate() {

        willThrow(MailSendException.class).given(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException()
            .isThrownBy(() -> adapter.notifyAccountLocked(Email.from(TO_ADDRESS), LOCKOUT_DURATION));
    }
}
