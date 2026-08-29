package com.valadir.security.redis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public record TokenFingerprint(String value) {

    private static final Pattern SHA_256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    public TokenFingerprint {

        if (value == null || !SHA_256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("A token fingerprint is the lowercase hex of a SHA-256 digest");
        }
    }

    public static TokenFingerprint of(String token) {

        return new TokenFingerprint(HexFormat.of().formatHex(sha256(token)));
    }

    private static byte[] sha256(String token) {

        try {

            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            // Every JVM is required to provide SHA-256: the catch exists because the API declares it
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }
}
