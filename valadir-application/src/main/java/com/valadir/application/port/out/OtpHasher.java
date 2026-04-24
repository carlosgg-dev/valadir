package com.valadir.application.port.out;

public interface OtpHasher {

    String hash(String plainCode);

    boolean matches(String plainCode, String hashedCode);
}
