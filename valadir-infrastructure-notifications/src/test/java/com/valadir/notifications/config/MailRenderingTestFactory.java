package com.valadir.notifications.config;

import com.valadir.notifications.mail.DurationWording;
import com.valadir.notifications.mail.MailContentRenderer;
import org.springframework.context.MessageSource;

/**
 * Hands the adapter tests the very collaborators production wires, built by {@link NotificationsWiring} itself.
 * Rebuilding an equivalent engine here would let the templates, the bundles or the resolver patterns drift while
 * every test stayed green.
 */
public final class MailRenderingTestFactory {

    private static final NotificationsWiring WIRING = new NotificationsWiring();
    private static final MessageSource MESSAGE_SOURCE = WIRING.mailMessageSource();

    private MailRenderingTestFactory() {

    }

    public static MailContentRenderer contentRenderer() {

        return WIRING.mailContentRenderer(WIRING.mailTemplateEngine(MESSAGE_SOURCE), MESSAGE_SOURCE);
    }

    public static DurationWording durationWording() {

        return WIRING.durationWording(MESSAGE_SOURCE);
    }
}
