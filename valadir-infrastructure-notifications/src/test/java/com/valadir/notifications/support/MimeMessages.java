package com.valadir.notifications.support;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.util.Optional;

/**
 * Readers over a composed message. A missing part throws with the type it looked for, so a test
 * that lost its HTML alternative says so instead of failing on a null further down.
 */
public final class MimeMessages {

    private static final String PLAIN_TEXT = "text/plain";
    private static final String HTML = "text/html";

    private MimeMessages() {

    }

    public static String plainTextOf(MimeMessage message) {

        return bodyOf(message, PLAIN_TEXT);
    }

    public static String htmlOf(MimeMessage message) {

        return bodyOf(message, HTML);
    }

    public static boolean carriesBothAlternatives(MimeMessage message) {

        return findPart(saved(message), PLAIN_TEXT).isPresent() && findPart(saved(message), HTML).isPresent();
    }

    private static String bodyOf(MimeMessage message, String mimeType) {

        return findPart(saved(message), mimeType)
            .orElseThrow(() -> new AssertionError("The message carries no " + mimeType + " part"));
    }

    /**
     * A composed message only gets its Content-Type headers written on save, which the real send
     * does for us. Without this every part still claims the default {@code text/plain}.
     */
    private static MimeMessage saved(MimeMessage message) {

        try {
            message.saveChanges();
            return message;
        } catch (MessagingException e) {
            throw new AssertionError("Could not save the composed message", e);
        }
    }

    private static Optional<String> findPart(Part part, String mimeType) {

        try {
            if (part.isMimeType(mimeType)) {
                return Optional.of(part.getContent().toString());
            }

            if (part.getContent() instanceof MimeMultipart multipart) {
                return findInMultipart(multipart, mimeType);
            }

            return Optional.empty();

        } catch (MessagingException | IOException e) {
            throw new AssertionError("Could not read the composed message", e);
        }
    }

    private static Optional<String> findInMultipart(MimeMultipart multipart, String mimeType) throws MessagingException {

        for (int i = 0; i < multipart.getCount(); i++) {
            var found = findPart(multipart.getBodyPart(i), mimeType);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }
}
