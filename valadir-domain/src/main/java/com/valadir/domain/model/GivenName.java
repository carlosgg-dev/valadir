package com.valadir.domain.model;

public record GivenName(String value) {

    public GivenName {

        if (value != null && value.isBlank()) {
            value = null;
        }
    }

    public static GivenName empty() {

        return new GivenName(null);
    }
}
