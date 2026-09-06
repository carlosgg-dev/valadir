package com.valadir.notifications.adapter;

import com.jayway.jsonpath.JsonPath;
import com.valadir.application.port.out.OtpNotification;
import com.valadir.domain.model.Email;
import com.valadir.domain.model.Language;
import com.valadir.domain.model.PlainOtp;
import com.valadir.notifications.config.MailRenderingTestFactory;
import com.valadir.notifications.mail.MimeMailSender;
import com.valadir.test.containers.MailpitContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AccountActivationNotifierJavaMailAdapterIT {

    private static final String FROM_ADDRESS = "noreply@valadir.local";
    private static final String TO_ADDRESS = "bruce.wayne@email.com";
    private static final PlainOtp OTP = PlainOtp.from("482913");
    private static final Duration OTP_TTL = Duration.ofMinutes(15);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private AccountActivationNotifierJavaMailAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {

        deleteDeliveredMessages();

        var mailSender = new JavaMailSenderImpl();
        mailSender.setHost(MailpitContainer.host());
        mailSender.setPort(MailpitContainer.smtpPort());

        adapter = new AccountActivationNotifierJavaMailAdapter(
            MailRenderingTestFactory.contentRenderer(),
            new MimeMailSender(mailSender, FROM_ADDRESS),
            MailRenderingTestFactory.durationWording()
        );
    }

    @Test
    void sendActivationCode_realSmtpServer_deliversBothAlternatives() throws Exception {

        var notification = new OtpNotification(Email.from(TO_ADDRESS), OTP, OTP_TTL, Language.ES);
        adapter.sendActivationCode(notification);

        var delivered = onlyDeliveredMessage();

        assertThat(JsonPath.<String>read(delivered, "$.Subject")).isEqualTo("Valadir - código de activación de cuenta");
        assertThat(JsonPath.<String>read(delivered, "$.To[0].Address")).isEqualTo(TO_ADDRESS);
        assertThat(JsonPath.<String>read(delivered, "$.From.Address")).isEqualTo(FROM_ADDRESS);

        // Mailpit reports the two alternatives apart, which is what proves the message arrived as a
        // multipart and not as a single body carrying the other one as text.
        assertThat(JsonPath.<String>read(delivered, "$.HTML")).contains(OTP.value(), "Activa tu cuenta", "15 minutos");
        assertThat(JsonPath.<String>read(delivered, "$.Text")).contains(OTP.value(), "15 minutos").doesNotContain("<table");
    }

    private static String onlyDeliveredMessage() throws Exception {

        var listing = get("/api/v1/messages");
        assertThat(JsonPath.<Integer>read(listing, "$.messages_count")).isEqualTo(1);

        return get("/api/v1/message/" + JsonPath.<String>read(listing, "$.messages[0].ID"));
    }

    private static void deleteDeliveredMessages() throws IOException, InterruptedException {

        send(HttpRequest.newBuilder(URI.create(MailpitContainer.apiBaseUrl() + "/api/v1/messages")).DELETE());
    }

    private static String get(String path) throws IOException, InterruptedException {

        return send(HttpRequest.newBuilder(URI.create(MailpitContainer.apiBaseUrl() + path)).GET());
    }

    private static String send(HttpRequest.Builder request) throws IOException, InterruptedException {

        var response = HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new AssertionError("Mailpit answered " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }
}
