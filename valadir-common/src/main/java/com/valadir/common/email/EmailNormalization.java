package com.valadir.common.email;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The single spelling an address resolves to. Shared because both sides key on it: the domain
 * resolves an account by the stored bytes, and the rate limiter counts attempts under them. A
 * form owned by one side alone would let the same address resolve to one account and two buckets.
 */
public final class EmailNormalization {

    private EmailNormalization() {

    }

    // Composed last, and after the lowercase: an account is resolved by bytes, so the two spellings
    // of an accented letter have to reach the column as one address.
    public static String canonicalOf(String value) {

        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }
}
