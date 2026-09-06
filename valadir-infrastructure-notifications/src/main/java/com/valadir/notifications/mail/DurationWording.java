package com.valadir.notifications.mail;

import org.springframework.context.MessageSource;

import java.time.Duration;
import java.util.Locale;

/**
 * Spells a duration out for a reader: an OTP lifetime, a lockout window. The unit and its plural
 * come from the bundle, so a language does not inherit English pluralisation.
 */
public class DurationWording {

    private final MessageSource messageSource;

    public DurationWording(MessageSource messageSource) {

        this.messageSource = messageSource;
    }

    // The value is configuration: truncating to minutes would announce a "0 minutes" lockout
    // as soon as one of the tiers drops below a minute.
    public String wordingOf(Duration duration, Locale locale) {

        long minutes = duration.toMinutes();

        return minutes > 0
            ? spellOut(minutes, "minute", locale)
            : spellOut(duration.toSeconds(), "second", locale);
    }

    private String spellOut(long amount, String unit, Locale locale) {

        var unitKey = "duration." + unit + (amount == 1 ? ".one" : ".other");

        // Locale.ROOT on the number: the reader compares these digits with what we told them, so
        // they must not come out in another numbering system.
        return String.format(Locale.ROOT, "%d %s", amount, messageSource.getMessage(unitKey, null, locale));
    }
}
